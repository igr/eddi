# Optimistic Check-and-Append for Command Consistency

**Date:** 2026-08-19
**Branch:** `check-and-append`
**Status:** Approved for implementation

## Motivation

Commands are not queued and not ordered. `AsyncCommandHandler` launches a coroutine per
command, so N concurrent HTTP requests run N command handlers in parallel, each in its own
`TxCommandHandler` transaction. Nothing prevents two commands that decide on the same facts
from both succeeding:

- Two `RegisterStudent` with the same email both pass `ensureUniqueEmail` (each transaction
  sees its own REPEATABLE READ snapshot; the events table has no uniqueness on payload data)
  and both store a `StudentRegistered`. The projection's `email UNIQUE` then rejects the
  second insert inside `DbEventProcessor`'s loop, which has no error handling — the polling
  coroutine dies and projections stall.
- The same holds for every guard: `PayTuition` × 2, `EnrollStudentInCourse` × 2, and so on.

The wrong fix is a command inbox that executes commands one by one. Arrival order between
unrelated commands is arbitrary anyway, a global queue serializes the whole system onto one
worker, and it needs leader election the moment a second instance runs. *Events* need a total
order and have one (`seq`). *Commands* need exactly one guarantee:

> A command must not succeed on facts that another command changed under it.

That is a per-id property, and it is what this design enforces — optimistically, at append
time, in the event store. Commands stay parallel and lock-free; a stale command is rejected
and re-run.

## Decisions

| # | Decision | Rationale |
|---|---|---|
| 1 | The consistency boundary of a command is the set of ids it *read* plus the ids of the event it *emits* | Matches the ids-as-boundary model (`etc/NoEntities.md`); no aggregates, no declarations |
| 2 | Per-id **version** = `seq` of the latest event carrying the id, `Seq.ZERO` if none | Reuses `seq` and the existing GIN-indexed `ids` column; `ZERO` already means "non-existing" (README) |
| 3 | Optimistic: the append rechecks the versions the command read; a mismatch is `ConcurrencyConflict` and the command is re-run | No locks held while a command runs; safe across instances; conflicts are rare and cheap |
| 4 | Appends are serialized **per id** with `pg_advisory_xact_lock` inside a READ COMMITTED transaction — conditional *and* unconditional appends alike | Makes "max seq per id" a gap-free version (see §2); a raw append that skipped the lock could slip past a recheck |
| 5 | Value uniqueness (email, course name) is expressed with value-derived ids carried by the event | Brings the two `findEvents(dataFilters)` guards inside the boundary without a new query kind |
| 6 | `TxCommandHandler` / `.tx()` is removed from the pipeline and deleted | A REPEATABLE READ wrapper freezes its snapshot before the append's lock is granted and defeats the recheck |
| 7 | Retry up to 3 times, then the conflict flows out as an ordinary `CommandError` | Bounded; a retry sees the winner's event and normally ends in a proper domain error |
| 8 | Reset the database (`just infra-reset`) rather than backfilling the new value ids | Dev database; consistent with the previous change |

## Design

### 1. Framework core (`eddi-api`)

```kotlin
typealias Versions = Map<Id, Seq>

interface EventStore : EventStoreInbox, EventStoreRepo {
    /** Version of [id]: the sequence of the latest event carrying it, [Seq.ZERO] if none. */
    fun versionOf(id: Id): Seq
}

interface EventStoreInbox {
    fun <E : Event> storeEvent(event: E, correlationId: ULong = 0u): EventEnvelope<E>       // unchanged
    /** Appends [event] only if every id in [expected] still has exactly that version. */
    fun <E : Event> storeEvent(event: E, expected: Versions, correlationId: ULong = 0u)
        : Either<ConcurrencyConflict, EventEnvelope<E>>
}

/** The ids whose version changed between the command's read and its append. */
data class ConcurrencyConflict(val stale: Set<Id>) : CommandError
```

`Versions` lives next to `Seq` in `models_event.kt`; `ConcurrencyConflict` next to
`UnknownCommandError` in `models_command.kt`. `EventStoreRepo` is **unchanged**: commands,
`StubEventStoreRepo`, and the reified lookup extensions keep their signatures.

**`EventStoreSession`** (new file `EventStoreSession.kt`) is a per-command `EventStoreRepo`
over an `EventStore`, created with `fun EventStore.session()`. It records what the command
read and appends conditionally on it:

```kotlin
class EventStoreSession(store: EventStore) : EventStoreRepo {
    fun <E : Event> append(event: E, correlationId: ULong = 0u): Either<ConcurrencyConflict, EventEnvelope<E>>
}
```

Session rules — these are load-bearing and must not be simplified away:

1. `findEventById` and `findEventByMultipleIds` call `store.versionOf(id)` for each id
   **before** delegating the read. Version-then-read is what makes any interleaving safe: a
   commit between the two turns into a spurious (harmless) conflict; a commit after the read
   is caught by the recheck. Read-then-version would let a stale read pass.
2. Versions are recorded **first-touch only** (`getOrPut`). A later read of the same id must
   not refresh the version, or a change between the two reads would be hidden.
3. A `null` result is still recorded (version `ZERO`). "No `TuitionPaid` for S yet" is a fact
   the command relies on.
4. `findEvents(name, dataFilters)` passes through **unrecorded** — it has no ids and is
   outside the boundary. Its KDoc says so.
5. `append(event)` = `store.storeEvent(event, expected = recorded versions)`. A session is
   single-shot: a second `append` is a programming error (`IllegalStateException`).

**Effects and helpers** — `retryOnConflict` in `Eddi.kt` next to `async()`; `session()` and
`EventStore.commandHandler` in `EventStoreSession.kt`:

```kotlin
/** Runs [decide] with a fresh session per command and appends the emitted event through it. */
fun <E : Event> EventStore.commandHandler(
    decide: (EventStoreRepo, Command) -> Either<CommandError, E>
): CommandHandler<EventEnvelope<E>>

/** Re-runs the command while it fails with [ConcurrencyConflict], at most [times] re-runs. */
fun <R> CommandHandler<R>.retryOnConflict(times: Int = 3): CommandHandler<R>
```

`EventStore.commandHandler` exists so the "one session, decide, append via that session"
shape lives in the framework rather than in every app's `when`. `retryOnConflict` is an
effect in the style of `.async()`: `times` is the number of *re*-runs after the first attempt,
each with a fresh session (the handler creates one per invocation); it passes every other
`Left` and every `Right` through untouched and prints one line per retry, matching the
existing `println` style.

`AsyncCommandHandler` is unchanged. `CommandProcessor`, `process {}`, `emit {}` are unchanged.

### 2. Postgres append (`eddi-db`)

`versionOf(id)`:

```sql
SELECT max(seq) FROM eddi.events WHERE ids @> '[{"StudentId":"<uuid>"}]'::jsonb
```

served by the existing GIN index `idx_events_ids`; `null` → `Seq.ZERO`.

Conditional `storeEvent(event, expected)` runs one transaction at **READ COMMITTED** — Exposed
`transaction(Connection.TRANSACTION_READ_COMMITTED) { … }` overrides the pool's REPEATABLE
READ default for this transaction only:

1. For every id in `event.ids() ∪ expected.keys`, sorted by lock key (deadlock-free):
   `SELECT pg_advisory_xact_lock(hashtextextended('<IdClass>:<uuid>', 0))`. The key uses the
   id's class simple name, the same name `Json.idsToNode` stores.
2. Re-read `versionOf` for each id in `expected` — a fresh snapshot that sees everything
   committed before the lock was granted. Any mismatch → `Left(ConcurrencyConflict(stale))`,
   nothing written.
3. Otherwise `INSERT` as today → `Right(envelope)`. Locks release at commit.

The unconditional `storeEvent(event, correlationId)` takes the same locks on `event.ids()`
and skips the recheck.

**Why the per-id lock makes the version sound.** `seq` is a `BIGSERIAL` assigned at insert
time, and a transaction with a lower `seq` can commit *after* one with a higher `seq`. So
"max visible seq" is *not* a safe global position. It *is* a safe per-id version once every
append that carries the id holds that id's lock until commit: for any single id, appends
cannot interleave, so `seq` order equals commit order for that id and its max seq is
gap-free. This is why the unconditional append must lock too, and why the recheck under the
lock is conclusive.

**Outer transactions.** The recheck must see committed data, so the *conditional* append must
not run inside an outer transaction (an outer REPEATABLE READ snapshot predates the lock).
The conditional path guards it: `check(TransactionManager.currentOrNull() == null)` with a
message that names the rule. The unconditional append is allowed inside an outer transaction
— its lock is then held until the outer commit, which keeps per-id append order intact.
Command reads need no wrapping transaction — each `dbFind*` already runs its own — and rule 1
in §1 covers the interleavings.

Files: `dbStoreEvent.kt` gains the conditional variant and the shared lock/insert helper;
new `dbVersionOf.kt`; `DbEventStore` implements `versionOf` and both `storeEvent`s;
`txCommandHandler.kt` is deleted. No schema change, no migration.

### 3. Domain (`example-events`)

`RegisterStudent` and `PublishCourse` guard uniqueness by a *value* through
`findEvents(dataFilters)`, which has no id and cannot be covered. Both move inside the
boundary with value-derived ids:

```kotlin
@JvmInline
value class EmailId(override val id: UUID) : Id {
    companion object {
        fun of(email: String) = EmailId(UUID.nameUUIDFromBytes("email:$email".toByteArray()))
    }
}

data class StudentRegistered(…) : Event {
    override fun ids() = listOf(studentId, EmailId.of(email))
}

fun ensureUniqueEmail(es: EventStoreRepo) = commandProcessor<RegisterStudent> {
    ensure(es.findEventById<StudentRegistered>(EmailId.of(it.email)) == null) {
        RegisterStudentError.StudentAlreadyExist
    }
}
```

`CourseNameId.of(courseName)` on `CoursePublished` / `ensureUniqueCourse` follows the same
shape. No normalization of the value: today's check is exact-match and this preserves it.

Two racing `RegisterStudent` with the same email now both record `EmailId → ZERO`; the
loser's append rechecks it, finds the winner's `seq`, retries with a fresh session, and fails
properly with `StudentAlreadyExist`. The other three commands are untouched: their emitted
events already carry every id they read (`EnrollStudentInCourse` reads S, K, S+K, S and
emits `{S, K}`).

The `ids` jsonb shape is unchanged (`[{"StudentId":…},{"EmailId":…}]`). Rows stored before
this change lack `EmailId`, so their emails are not protected — reset the database.

### 4. Wiring (`example`)

```kotlin
object Main {
    val es = DbEventStore()
    val launch = commandHandler(es).retryOnConflict().async()      // .tx() gone
}

fun commandHandler(es: EventStore) = es.commandHandler { s, command ->
    when (command) {
        is RegisterStudent -> command(s)
        is UpdateStudent -> command(s)
        is PayTuition -> command(s)
        is PublishCourse -> command(s)
        is EnrollStudentInCourse -> command(s)
        else -> Either.Left(UnknownCommandError(command))
    }
}
```

`DbEventProcessor`, projections, API endpoints and UI are unchanged.

### 5. Error handling

- **Conflict** → re-run with a fresh session, default 3 re-runs. After the last one,
  `Left(ConcurrencyConflict)` flows out like any `CommandError` and is logged and dropped by
  the async handler, exactly as errors are today (the API's `202 Accepted` already means
  "accepted, outcome unknown").
- **Conditional append inside an outer transaction** → `IllegalStateException`. Programming
  error, fail fast.
- **Second `append` on a session** → `IllegalStateException`.
- Database failures propagate as today.

### 6. Testing

Tests are written before the corresponding production change wherever the seam exists.

**`eddi-api` — unit, against a fake `EventStore`:**
- Session records ids with the version taken *before* the delegated read (the fake records
  call order and asserts `versionOf` precedes the find).
- First-touch version wins on repeated reads of the same id.
- `null` reads are recorded as `ZERO`; `findEvents` is not recorded.
- `append` passes exactly the recorded versions to `storeEvent(expected)`; a second `append`
  throws.
- `retryOnConflict`: re-runs only on `ConcurrencyConflict`, at most `times` re-runs, and
  passes other `Left`s and all `Right`s through untouched.
- `EventStore.commandHandler`: one session per invocation, appended through that session.

**`eddi-db` — integration, Testcontainers `postgres:18-alpine`** (the compose image). New
test dependencies `org.testcontainers:postgresql` and `org.testcontainers:junit-jupiter`, plus
the JUnit wiring `eddi-api` already uses. `Db(...)` connects and runs the `eddi` migration;
the test registers a private event class.
- `versionOf` is `ZERO` with no events and the max seq across events carrying the id,
  including multi-id events.
- Conditional append succeeds when versions match; fails naming the stale id when a newer
  event carrying it exists.
- Two threads racing from the same expected version → exactly one `Right`, the other
  `Left(ConcurrencyConflict)` naming the id.
- Conditional append inside an outer transaction throws.
- Unconditional `storeEvent` still returns the envelope.

**`example-events` — processor tests:**
- `StudentRegistered.ids()` and `CoursePublished.ids()` include the value id.
- `RegisterStudent` fails when a `StudentRegistered` with the same `EmailId` exists and
  succeeds when only a different email exists. This needs `StubEventStoreRepo.findEventById`
  to match ids (today it matches by name only) — a small stub improvement.

**Manual:** `just run`, fire ~20 concurrent identical `POST /api/students`; expect exactly one
row in `college.student` and no dead projection loop.

### 7. Migration

None for the schema. `just infra-reset` for the value ids (§3).

## Out of Scope

Noticed during design, deliberately excluded:

- **Durable command inbox** — persisting accepted commands, parallel workers claiming rows
  with `FOR UPDATE SKIP LOCKED`, and a job-status API for the `202` responses. Orthogonal to
  correctness and layerable later without touching this mechanism.
- **Poller seq-gap in `DbEventProcessor`.** `seq > last_seq` can skip an event whose lower
  `seq` commits after a higher one was consumed. Same root cause as the position note in
  §2, needs its own spec (a watermark on in-flight transactions, or a global append lock).
- **`findEvents(dataFilters)`** — after §3 it has no production callers. Left in place,
  documented as outside the boundary; removing it is a separate cleanup.
- **`AsyncCommandHandler` job tracking** (`jobs` map, expiry) — unchanged.

## Risks

| Risk | Mitigation |
|---|---|
| A command is invoked with the raw store instead of a session, silently losing protection | `EventStore.commandHandler` is the only wiring shown and used; session KDoc states the rule |
| A guard reads through `findEvents(dataFilters)` and is unprotected | KDoc on `findEvents`; the two existing uses are converted in §3 |
| Recheck runs inside an outer transaction and sees a stale snapshot | Runtime guard fails fast; `TxCommandHandler` deleted |
| Version-then-read or first-touch invariant "simplified" during implementation | Unit tests in §6 assert call order and first-touch explicitly |
| Testcontainers needs Docker in the environment | Docker is already required for the compose database |
| Retry storms under heavy contention on one id | Bounded by `times`; each retry is a full, cheap re-decision |
