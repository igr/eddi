# UUID Identity for Tags

**Date:** 2026-08-15
**Branch:** `id`
**Status:** Approved for implementation

## Motivation

The trigger was a one-line requirement: *`StudentRegistered` must contain `StudentId`.*

That is impossible under the current model. `StudentId` is defined as the sequence
number of the `StudentRegistered` event itself:

```kotlin
fun EventEnvelope<StudentRegistered>.tag() = StudentId(this.sequence)
```

`Seq` is a Postgres `BIGSERIAL`, assigned at `INSERT` time in `dbStoreEvent.kt` — *after*
the event has been serialized into `DbEvents.data`. An event cannot contain a value that
only comes into existence as a side effect of storing that event.

Resolving this means identity stops being a log position and becomes a real, independent
identifier minted before anything is written.

## Decisions

| # | Decision | Rationale |
|---|---|---|
| 1 | `Tag.id` is a `UUID`, replacing `Tag.seq: Seq` | Identity independent of log position |
| 2 | Uniform UUID — every event with a `Tag` type mints and carries its own id | One identity concept; the update chain stays expressible as a `Ref` |
| 3 | Commands carry the id; the caller mints it | Identity is decided before storage, making commands retry-safe: a resent command reuses its id instead of creating a duplicate entity |
| 4 | Events self-tag via `refs()` | Replaces the primary-key lookup that identity-as-seq made possible |
| 5 | Drop and recreate the database | Every id and the `tags` format change at once |

`Seq` itself is **unchanged**. It remains the event log position on `EventEnvelope`, used
for ordering and provenance. Only *identity* moves off it.

## Design

### 1. Framework core (`eddi-api`)

```kotlin
interface Tag<out Event> { val id: UUID }           // was: val seq: Seq
data class Ref(val name: EventName, val id: UUID)   // was: seq: Seq
```

`Events.refOf(tag)` becomes `Ref(klassToName[tag::class]!!, tag.id)`. `Events.register`,
`EventMeta`, `EventName`, and `EventEnvelope` are otherwise untouched.

**Self-tagging is the load-bearing change.** Today the existence guards do a primary-key
lookup, which only works because identity *is* the sequence:

```kotlin
es.findEvent<StudentRegistered>(it.student.seq, StudentRegisteredEvent.NAME)
```

With a UUID there is no such lookup, so every event that has a `Tag` type must reference
its own id:

```kotlin
object StudentRegisteredEvent : EventMeta<StudentRegistered> {
    override fun refs(event: StudentRegistered) = arrayOf(Ref(NAME, event.studentId.id))
}
```

The four `ensure*Exists` guards then become `findEventByTag(NAME, tag)`. This leaves
`findEvent(seq, name)` with zero callers, so it is removed from `EventStoreRepo`.

Only the three events that have a tag type mint ids: `StudentRegistered`,
`CoursePublished`, `StudentUpdated`. `TuitionPaid` and `StudentEnrolledInCourse` are only
ever found *by* the student/course tag, never by an id of their own, so they stay id-free.

### 2. Serialization (`eddi-json`)

`RefSerializer` / `RefDeserializer` move from numeric to string values:

```
[{"StudentRegistered": "3f2a…"}]      // was: [{"StudentRegistered": 42}]
```

`Json.kt:65-148` holds an ~80-line reflection-based event deserializer that exists because
of *double* value-class nesting (`Tag` → `Seq` → `ULong`), which flattens to a primitive
`long` and defeats both Jackson's Kotlin module and Kotlin reflection.

`StudentId(UUID)` is a single level of value-class nesting around a reference type, which
Jackson normally handles. That path is therefore **likely deletable** — but this is a
hypothesis, not a fact. Plan: port it to UUID, add a round-trip test, and only then attempt
deletion, reverting if the test fails. Do not delete it speculatively.

### 3. Query layer (`eddi-db`)

The jsonb containment needle becomes string-valued in `dbFindEventByTag` and
`dbFindEventByMultipleTags`:

```kotlin
val needle = """[{"$key": "$value"}]"""   // value now quoted
```

The GIN index on `eddi.events(tags)` continues to work unchanged.

**Pre-existing issue, deliberately not fixed:** `dbFindLastEventByTag` filters on
`ref.name` and `sequence` but never uses the ref's value, so it matches *any* event of that
name. `findLastEventByTagBefore` has no callers. Changing it would alter untested behavior
outside this change's purpose; it is noted here instead.

### 4. Domain (`example-events`)

Tags become UUID-backed and commands carry ids:

```kotlin
@JvmInline value class StudentId(override val id: UUID) : Tag<StudentRegistered>

data class RegisterStudent(
    val studentId: StudentId,
    val firstName: String, val lastName: String, val email: String
) : Command
```

`tag()` extensions invert — they read from the payload rather than deriving from the
envelope:

```kotlin
fun EventEnvelope<StudentRegistered>.tag() = event.studentId   // was: StudentId(this.sequence)
```

`UpdateStudent` gains an `updateId`. `StudentUpdated.last` becomes the previous update's
UUID, still derived server-side inside `emit` as established earlier — the lookup now reads
`updateId` from the found envelope's payload instead of using its sequence.

### 5. Projections and schema (`example`)

`student.id` and `course.id` are already UUID primary keys. They stop being
database-generated and become the domain id, collapsing today's two parallel identifiers
(`id` UUID + `seq`) into one.

| Change | Detail |
|---|---|
| `student.id` / `course.id` | drop `DEFAULT gen_random_uuid()`, insert the domain id |
| `student.last` | `BIGINT` → `UUID`; FK to `eddi.events` dropped (update ids are not rows) |
| `seq` columns | retained as event provenance, no longer used for lookup |
| `course_enrolled` | FKs already UUID — now point at real domain ids |
| `dbUpdateStudent`, `dbUpdateTuitionPayment` | key on `id` instead of `seq` |
| `dbInsertEnrollment` | both seq→UUID resolution queries deleted; ids are already the FK values |
| API (5 endpoints) | mint UUIDs on create; drop all `seq`→tag conversions |
| UI | routes already use `student.id` — minimal churn |

### 6. Migration

Drop and recreate via `just infra-reset`. Every id and the `tags` format change
simultaneously; in-place migration of demo data is not worth the machinery.

Because the database is wiped, `V1`/`V2` are edited in place rather than adding a `V3`.
This would break Flyway checksums on a surviving database, which is acceptable only
because no database survives. **`just infra-reset` is mandatory, not optional** — a stale
volume will fail validation.

### 7. Testing

The repository currently has **no tests**. `eddi-json` configures `useJUnitPlatform()` but
declares no JUnit dependency and has no test sources.

This change touches serialization and identity semantics across four modules, so tests are
added as part of it. The catalog already declares `junit-jupiter-engine` (currently unused
by any module); writing tests also needs `junit-jupiter-api`, so the catalog gains that
entry and both `eddi-json` and `example-events` gain `testImplementation` wiring.

Two seams carry nearly all the risk:

1. **JSON round-trips** (`eddi-json`) — `Ref` to/from jsonb, and every event with tag
   properties surviving serialize → deserialize with ids intact, covering non-null tags,
   nullable tags (`StudentUpdated.last`), and defaulted parameters (`registeredAt`).
2. **Command processors** (`example-events`) — against a stub `EventStoreRepo`, asserting
   emitted events, derived `last` chain links, and each error path.

Tests are written before the corresponding production change wherever the seam already
exists, per the project's TDD practice.

## Out of Scope

Noticed during design, deliberately excluded:

- **`Tag<out Event>` type-parameter shadowing.** The parameter is literally named `Event`,
  shadowing the `Event` interface, so `Tag` is in fact unconstrained. Tightening it to
  `Tag<out E : Event>` is a separate, unrelated cleanup.
- **`Student.last` is now unread by any UI**, after the optimistic-locking removal in the
  previous change. Left in the projection model.
- **`dbFindLastEventByTag` ignoring its ref value**, as described in §3.

## Risks

| Risk | Mitigation |
|---|---|
| The reflection deserializer breaks on UUID-backed tags | Round-trip tests before touching it; port rather than delete |
| A stale Postgres volume fails Flyway checksum validation | `just infra-reset` is a required step, called out in §6 |
| Self-tagging missed on an event, silently breaking existence guards | Processor tests assert the guards against a stub repo |
| Uniform-UUID churn touches many files at once | Module-by-module ordering: `eddi-api` → `eddi-json` → `eddi-db` → `example-events` → `example`, compiling at each step |
