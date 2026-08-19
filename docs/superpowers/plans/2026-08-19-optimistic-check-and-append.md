# Optimistic Check-and-Append Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make concurrent commands safe without ordering them: the event store refuses an append when any id the command read has changed since it read it, and the command is re-run.

**Architecture:** Each command runs against an `EventStoreSession` — a per-command `EventStoreRepo` that records the *version* (max `seq`) of every id it reads, before reading it. The session appends the emitted event through a conditional `storeEvent(event, expected)` that, inside one READ COMMITTED transaction, takes a `pg_advisory_xact_lock` per id, rechecks the versions and inserts only if nothing changed. A mismatch is `ConcurrencyConflict`, and `retryOnConflict` re-runs the command with a fresh session. Value uniqueness (email, course name) becomes an id lookup via value-derived ids so it sits inside the boundary.

**Tech Stack:** Kotlin 2.2 / JVM 21, Arrow 2.2 (`Either`), Exposed 0.56 (JDBC, raw `exec` for advisory locks), PostgreSQL 18 (`hashtextextended`, jsonb `@>` on the GIN-indexed `ids` column), JUnit 5, Testcontainers (`org.testcontainers:postgresql`, image `postgres:18-alpine`).

**Spec:** `docs/superpowers/specs/2026-08-19-optimistic-check-and-append-design.md`

## Global Constraints

- No schema change and no Flyway migration; the `ids` jsonb shape `[{"<IdClass>":"<uuid>"}, …]` is unchanged.
- Conditional append runs in its own transaction at `Connection.TRANSACTION_READ_COMMITTED`; it takes `pg_advisory_xact_lock(hashtextextended('<IdClass>:<uuid>', 0))` for every id in `event.ids() ∪ expected.keys`, sorted by that key string, **before** rechecking versions; it refuses to run inside an outer transaction (`check(TransactionManager.currentOrNull() == null)`).
- The unconditional `storeEvent` takes the same locks on `event.ids()` (no recheck).
- Session rules: version is read **before** the delegated read; **first touch wins**; a `null` read is recorded as `Seq.ZERO`; `findEvents(dataFilters)` is **not** recorded; a session appends **at most once**.
- `retryOnConflict(times = 3)`: `times` = number of re-runs after the first attempt; re-run only on `ConcurrencyConflict`; every other result passes through untouched.
- Log lines use the existing `println` + emoji style.
- `EventStoreRepo`'s method set is unchanged; `TxCommandHandler` / `.tx()` is deleted.
- Tests are written before the production change in every task; commit at the end of every task; run `./gradlew build` before the final commit of Task 9.
- Commit messages: imperative sentence in the repo's style; end with `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.

---

## File Structure

| File | Responsibility |
|---|---|
| `eddi-api/src/main/kotlin/dev/oblac/eddi/models_event.kt` | + `typealias Versions = Map<Id, Seq>` |
| `eddi-api/src/main/kotlin/dev/oblac/eddi/models_command.kt` | + `ConcurrencyConflict` |
| `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStore.kt` | + `versionOf(id)` |
| `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreInbox.kt` | + conditional `storeEvent(event, expected, correlationId)` |
| `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreRepo.kt` | KDoc: `findEvents` is outside the boundary |
| `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreSession.kt` (new) | `EventStoreSession`, `EventStore.session()`, `EventStore.commandHandler(decide)` |
| `eddi-api/src/main/kotlin/dev/oblac/eddi/Eddi.kt` | + `CommandHandler<R>.retryOnConflict(times)` |
| `eddi-api/src/test/kotlin/dev/oblac/eddi/FakeEventStore.kt` (new) | In-memory `EventStore` + fixtures for session/effect tests |
| `eddi-api/src/test/kotlin/dev/oblac/eddi/EventStoreSessionTest.kt` (new) | Session rules |
| `eddi-api/src/test/kotlin/dev/oblac/eddi/SessionCommandHandlerTest.kt` (new) | `EventStore.commandHandler` |
| `eddi-api/src/test/kotlin/dev/oblac/eddi/RetryOnConflictTest.kt` (new) | `retryOnConflict` |
| `gradle/libs.versions.toml`, `eddi-db/build.gradle.kts` | Testcontainers + JUnit test wiring for `eddi-db` |
| `eddi-db/src/test/kotlin/dev/oblac/eddi/db/TestDb.kt` (new) | Singleton Postgres container, `Db`, fixture registration, `reset()` |
| `eddi-db/src/test/kotlin/dev/oblac/eddi/db/fixtures.kt` (new) | `PingId`, `Pinged`, `Ponged` |
| `eddi-db/src/main/kotlin/dev/oblac/eddi/db/idsContain.kt` (new) | `ids @> '[…]'::jsonb` op, shared by lookups and `dbVersionOf` |
| `eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbFindEventById.kt` | Use `idsContain` |
| `eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbVersionOf.kt` (new) | `dbVersionOf(id): Seq` |
| `eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbStoreEvent.kt` | Locked unconditional append, `dbStoreEventIf`, lock/insert helpers |
| `eddi-db/src/main/kotlin/dev/oblac/eddi/db/DbEventStore.kt` | Implements `versionOf` and both `storeEvent`s |
| `eddi-db/src/main/kotlin/dev/oblac/eddi/db/txCommandHandler.kt` | **deleted** |
| `eddi-db/src/test/kotlin/dev/oblac/eddi/db/DbVersionOfTest.kt`, `DbFindEventByIdTest.kt`, `DbStoreEventIfTest.kt`, `DbEventStoreTest.kt` (new) | Integration tests |
| `example-events/src/main/kotlin/dev/oblac/eddi/example/college/registerStudent.kt` | `EmailId`, `StudentRegistered.ids()`, `ensureUniqueEmail` |
| `example-events/src/main/kotlin/dev/oblac/eddi/example/college/publishCourse.kt` | `CourseNameId`, `CoursePublished.ids()`, `ensureUniqueCourse` |
| `example-events/src/test/kotlin/dev/oblac/eddi/example/college/StubEventStoreRepo.kt` | Match ids, not just names |
| `example-events/src/test/kotlin/dev/oblac/eddi/example/college/CommandProcessorTest.kt` | New/updated processor tests |
| `example/src/main/kotlin/dev/oblac/eddi/example/college/commandHandler.kt` | `es.commandHandler { s, command -> … }` |
| `example/src/main/kotlin/dev/oblac/eddi/example/college/main.kt` | `commandHandler(es).retryOnConflict().async()` |

---

### Task 1: `eddi-db` test infrastructure, `idsContain`, `dbVersionOf`

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `eddi-db/build.gradle.kts`
- Create: `eddi-db/src/test/kotlin/dev/oblac/eddi/db/fixtures.kt`
- Create: `eddi-db/src/test/kotlin/dev/oblac/eddi/db/TestDb.kt`
- Create: `eddi-db/src/test/kotlin/dev/oblac/eddi/db/DbFindEventByIdTest.kt`
- Create: `eddi-db/src/test/kotlin/dev/oblac/eddi/db/DbVersionOfTest.kt`
- Create: `eddi-db/src/main/kotlin/dev/oblac/eddi/db/idsContain.kt`
- Modify: `eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbFindEventById.kt`
- Create: `eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbVersionOf.kt`

**Interfaces:**
- Consumes: existing `dbStoreEvent(correlationId: ULong, event: E, eventName: EventName, ids: List<Id>): EventEnvelope<E>`, `dbFindEventById(eventName, id)`, `dbFindEventByMultipleIds(eventName, vararg ids)`, `Db(jdbcUrl, username, password)`, `Events.register(...)`.
- Produces: `internal fun idsContain(id: Id): Op<Boolean>`; `fun dbVersionOf(id: Id): Seq`; test helpers `TestDb.reset()`, fixtures `PingId`, `Pinged(ping)`, `Ponged(ping, other)`.

Note: the spec lists `org.testcontainers:junit-jupiter` too; it is not needed with the singleton-container pattern used here, so only `org.testcontainers:postgresql` is added.

Note: Testcontainers must be ≥ 1.21.4 — earlier releases pin Docker API 1.32, which Docker Engine 29+ rejects (`client version 1.32 is too old`).

- [x] **Step 1: Add Testcontainers and JUnit wiring to `eddi-db`**

In `gradle/libs.versions.toml` add under `[versions]`:

```toml
testcontainers = "1.21.4"
```

and under `[libraries]`:

```toml
testcontainers-postgresql = { module = "org.testcontainers:postgresql", version.ref = "testcontainers" }
```

In `eddi-db/build.gradle.kts` append inside `dependencies { … }`:

```kotlin
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.testcontainers.postgresql)
```

- [x] **Step 2: Create test fixtures and the singleton database**

`eddi-db/src/test/kotlin/dev/oblac/eddi/db/fixtures.kt`:

```kotlin
package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.Id
import java.util.UUID

// Public (not private) on purpose: the events are serialized with Jackson and read back by class name.

@JvmInline
value class PingId(override val id: UUID) : Id

data class Pinged(val ping: PingId) : Event {
    override fun ids() = listOf(ping)
}

data class Ponged(val ping: PingId, val other: PingId) : Event {
    override fun ids() = listOf(ping, other)
}
```

`eddi-db/src/test/kotlin/dev/oblac/eddi/db/TestDb.kt`:

```kotlin
package dev.oblac.eddi.db

import dev.oblac.eddi.Events
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * One Postgres container for the whole test JVM (Testcontainers' Ryuk removes it at exit).
 * Touching this object starts the container, connects Exposed through [Db] (which also runs
 * the `eddi` Flyway migration) and registers the test events.
 */
object TestDb {
    private val container = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:18-alpine")).also { it.start() }

    val db: Db = Db(container.jdbcUrl, container.username, container.password)

    init {
        Events.register(Pinged::class, Ponged::class)
    }

    /** Empties the event log and restarts `seq` at 1. */
    fun reset() {
        transaction {
            exec("TRUNCATE eddi.events, eddi.events_offsets RESTART IDENTITY CASCADE")
        }
    }
}
```

- [x] **Step 3: Write the characterization tests for id lookups and the failing tests for `dbVersionOf`**

`eddi-db/src/test/kotlin/dev/oblac/eddi/db/DbFindEventByIdTest.kt`:

```kotlin
package dev.oblac.eddi.db

import dev.oblac.eddi.EventName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DbFindEventByIdTest {

    private val pinged = EventName.of(Pinged::class)
    private val ponged = EventName.of(Ponged::class)

    @BeforeEach
    fun reset() = TestDb.reset()

    @Test
    fun `findEventById matches by id, not just by event name`() {
        val a = PingId(UUID.randomUUID())
        val b = PingId(UUID.randomUUID())
        val storedA = dbStoreEvent(0u, Pinged(a), pinged, listOf(a))
        dbStoreEvent(0u, Pinged(b), pinged, listOf(b))

        assertEquals(storedA.sequence, dbFindEventById(pinged, a)!!.sequence)
        assertNull(dbFindEventById(pinged, PingId(UUID.randomUUID())))
    }

    @Test
    fun `findEventByMultipleIds requires every id`() {
        val a = PingId(UUID.randomUUID())
        val b = PingId(UUID.randomUUID())
        val c = PingId(UUID.randomUUID())
        val storedAb = dbStoreEvent(0u, Ponged(a, b), ponged, listOf(a, b))

        assertEquals(storedAb.sequence, dbFindEventByMultipleIds(ponged, a, b)!!.sequence)
        assertNull(dbFindEventByMultipleIds(ponged, a, c))
    }
}
```

`eddi-db/src/test/kotlin/dev/oblac/eddi/db/DbVersionOfTest.kt`:

```kotlin
package dev.oblac.eddi.db

import dev.oblac.eddi.EventName
import dev.oblac.eddi.Seq
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DbVersionOfTest {

    private val pinged = EventName.of(Pinged::class)
    private val ponged = EventName.of(Ponged::class)

    @BeforeEach
    fun reset() = TestDb.reset()

    @Test
    fun `version is ZERO when no event carries the id`() {
        assertEquals(Seq.ZERO, dbVersionOf(PingId(UUID.randomUUID())))
    }

    @Test
    fun `version is the sequence of the latest event carrying the id, including multi-id events`() {
        val a = PingId(UUID.randomUUID())
        val b = PingId(UUID.randomUUID())
        dbStoreEvent(0u, Pinged(a), pinged, listOf(a))
        val second = dbStoreEvent(0u, Ponged(a, b), ponged, listOf(a, b))
        val third = dbStoreEvent(0u, Pinged(b), pinged, listOf(b))

        assertEquals(second.sequence, dbVersionOf(a))
        assertEquals(third.sequence, dbVersionOf(b))
    }
}
```

- [x] **Step 4: Run the tests to verify they fail to compile**

Run: `./gradlew :eddi-db:test --tests 'dev.oblac.eddi.db.DbVersionOfTest'`
Expected: FAIL — `Unresolved reference: dbVersionOf` (the `DbFindEventByIdTest` compiles and would pass, but the module's test compilation fails as a whole).

- [x] **Step 5: Implement `idsContain`, use it in the lookups, implement `dbVersionOf`**

`eddi-db/src/main/kotlin/dev/oblac/eddi/db/idsContain.kt`:

```kotlin
package dev.oblac.eddi.db

import dev.oblac.eddi.Id
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.json.Json
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.stringLiteral

/**
 * `ids @> '[{"<IdClass>": "<uuid>"}]'::jsonb` — the event carries [id].
 * Served by the GIN index on `eddi.events.ids`.
 */
internal fun idsContain(id: Id): Op<Boolean> = object : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append(DbEvents.ids)
        queryBuilder.append(" @> ")
        queryBuilder.append(stringLiteral(Json.idsToNode(listOf(id)).toString()))
        queryBuilder.append("::jsonb")
    }
}
```

Replace the whole of `eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbFindEventById.kt` with:

```kotlin
package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventName
import dev.oblac.eddi.Id
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.db.tables.toEventEnvelope
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun dbFindEventById(eventName: EventName, id: Id): EventEnvelope<Event>? = transaction {
    addLogger(StdOutSqlLogger)
    DbEvents
        .selectAll()
        .where { DbEvents.name eq eventName.value }
        .andWhere { idsContain(id) }
        .orderBy(DbEvents.sequence, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}


fun dbFindEventByMultipleIds(eventName: EventName, vararg ids: Id): EventEnvelope<Event>? = transaction {
    addLogger(StdOutSqlLogger)
    DbEvents
        .selectAll()
        .where { DbEvents.name eq eventName.value }
        .apply {
            ids.forEach { id -> andWhere { idsContain(id) } }
        }
        .orderBy(DbEvents.sequence, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}
```

`eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbVersionOf.kt`:

```kotlin
package dev.oblac.eddi.db

import dev.oblac.eddi.Id
import dev.oblac.eddi.Seq
import dev.oblac.eddi.db.tables.DbEvents
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Version of [id]: the sequence of the latest event carrying it, [Seq.ZERO] if none.
 * Inside an outer transaction this joins it (Exposed nests), which is what the conditional
 * append relies on to recheck versions under its locks.
 */
fun dbVersionOf(id: Id): Seq = transaction {
    val maxSeq = DbEvents.sequence.max()
    DbEvents
        .select(maxSeq)
        .where { idsContain(id) }
        .single()[maxSeq]
        ?.let { Seq(it) }
        ?: Seq.ZERO
}
```

- [x] **Step 6: Run the `eddi-db` tests to verify they pass**

Run: `./gradlew :eddi-db:test`
Expected: BUILD SUCCESSFUL; `DbFindEventByIdTest` (2 tests) and `DbVersionOfTest` (2 tests) pass. The first run pulls `postgres:18-alpine` if not cached and starts one container (≈10 s).

- [x] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml eddi-db/build.gradle.kts eddi-db/src/test eddi-db/src/main/kotlin/dev/oblac/eddi/db/idsContain.kt eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbFindEventById.kt eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbVersionOf.kt
git commit -m "Add dbVersionOf and Testcontainers-based eddi-db tests; share the ids-containment op

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: `Versions`, `ConcurrencyConflict`, and the locked conditional append

**Files:**
- Modify: `eddi-api/src/main/kotlin/dev/oblac/eddi/models_event.kt`
- Modify: `eddi-api/src/main/kotlin/dev/oblac/eddi/models_command.kt`
- Create: `eddi-db/src/test/kotlin/dev/oblac/eddi/db/DbStoreEventIfTest.kt`
- Modify: `eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbStoreEvent.kt`

**Interfaces:**
- Consumes: `dbVersionOf(id)` (Task 1), fixtures and `TestDb` (Task 1).
- Produces: `typealias Versions = Map<Id, Seq>`; `data class ConcurrencyConflict(val stale: Set<Id>) : CommandError`; `fun <E : Event> dbStoreEventIf(correlationId: ULong, event: E, eventName: EventName, ids: List<Id>, expected: Versions): Either<ConcurrencyConflict, EventEnvelope<E>>`; `dbStoreEvent(...)` keeps its signature but now takes the per-id locks.

- [x] **Step 1: Write the failing tests**

`eddi-db/src/test/kotlin/dev/oblac/eddi/db/DbStoreEventIfTest.kt`:

```kotlin
package dev.oblac.eddi.db

import dev.oblac.eddi.ConcurrencyConflict
import dev.oblac.eddi.EventName
import dev.oblac.eddi.Seq
import dev.oblac.eddi.Versions
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DbStoreEventIfTest {

    private val pinged = EventName.of(Pinged::class)

    @BeforeEach
    fun reset() = TestDb.reset()

    @Test
    fun `append succeeds when the expected versions are current`() {
        val id = PingId(UUID.randomUUID())

        val result = dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), mapOf(id to Seq.ZERO))

        assertTrue(result.isRight(), result.toString())
        assertEquals(result.getOrNull()!!.sequence, dbVersionOf(id))
    }

    @Test
    fun `append succeeds on a matching non-zero version`() {
        val id = PingId(UUID.randomUUID())
        dbStoreEvent(0u, Pinged(id), pinged, listOf(id))

        val result = dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), mapOf(id to dbVersionOf(id)))

        assertTrue(result.isRight(), result.toString())
    }

    @Test
    fun `append fails naming the stale id and stores nothing`() {
        val id = PingId(UUID.randomUUID())
        val first = dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), mapOf(id to Seq.ZERO)).getOrNull()!!

        val second = dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), mapOf(id to Seq.ZERO))

        assertEquals(ConcurrencyConflict(setOf(id)), second.leftOrNull())
        assertEquals(first.sequence, dbVersionOf(id))
    }

    @Test
    fun `append rechecks every expected id, not only the ids the event carries`() {
        val read = PingId(UUID.randomUUID())
        val emitted = PingId(UUID.randomUUID())
        dbStoreEvent(0u, Pinged(read), pinged, listOf(read))   // `read` changed after it was "read" at ZERO

        val result = dbStoreEventIf(0u, Pinged(emitted), pinged, listOf(emitted), mapOf(read to Seq.ZERO))

        assertEquals(ConcurrencyConflict(setOf(read)), result.leftOrNull())
        assertEquals(Seq.ZERO, dbVersionOf(emitted))
    }

    @Test
    fun `two appends racing from the same version - exactly one succeeds`() {
        val id = PingId(UUID.randomUUID())
        val expected: Versions = mapOf(id to Seq.ZERO)
        val gate = CyclicBarrier(2)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val results = (1..2).map {
                pool.submit(Callable {
                    gate.await()
                    dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), expected)
                })
            }.map { it.get(30, TimeUnit.SECONDS) }

            assertEquals(1, results.count { it.isRight() }, results.toString())
            assertEquals(ConcurrencyConflict(setOf(id)), results.single { it.isLeft() }.leftOrNull())
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `conditional append refuses to run inside an outer transaction`() {
        val id = PingId(UUID.randomUUID())

        val error = assertThrows<IllegalStateException> {
            transaction {
                dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), mapOf(id to Seq.ZERO))
            }
        }

        assertTrue(error.message!!.contains("outer transaction"), error.message)
        assertEquals(Seq.ZERO, dbVersionOf(id))
    }

    @Test
    fun `unconditional append still returns the stored envelope`() {
        val id = PingId(UUID.randomUUID())

        val stored = dbStoreEvent(7u, Pinged(id), pinged, listOf(id))

        assertEquals(pinged, stored.eventName)
        assertEquals(7uL, stored.correlationId)
        assertEquals(stored.sequence, dbVersionOf(id))
    }
}
```

- [x] **Step 2: Run the tests to verify they fail to compile**

Run: `./gradlew :eddi-db:test --tests 'dev.oblac.eddi.db.DbStoreEventIfTest'`
Expected: FAIL — `Unresolved reference: ConcurrencyConflict` / `Versions` / `dbStoreEventIf`.

- [x] **Step 3: Add `Versions` and `ConcurrencyConflict` to `eddi-api`**

Append to `eddi-api/src/main/kotlin/dev/oblac/eddi/models_event.kt`:

```kotlin

/**
 * Versions of ids as read by a command: for each id, the sequence of the latest event carrying it
 * ([Seq.ZERO] if none) at the time the command read it.
 */
typealias Versions = Map<Id, Seq>
```

Append to `eddi-api/src/main/kotlin/dev/oblac/eddi/models_command.kt`:

```kotlin

/**
 * The append was refused: the version of the [stale] ids changed between the command's read and
 * its append. The command should be re-run against the current state.
 */
data class ConcurrencyConflict(val stale: Set<Id>) : CommandError
```

- [x] **Step 4: Replace `dbStoreEvent.kt` with the locked appends**

Replace the whole of `eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbStoreEvent.kt` with:

```kotlin
package dev.oblac.eddi.db

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import dev.oblac.eddi.*
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.json.Json
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.time.Instant

/**
 * Stores [event] unconditionally. Still takes the per-id append lock on every id the event carries,
 * so that appends for any single id never interleave (see [lockIds]).
 */
fun <E : Event> dbStoreEvent(correlationId: ULong, event: E, eventName: EventName, ids: List<Id>): EventEnvelope<E> =
    transaction(Connection.TRANSACTION_READ_COMMITTED) {
        lockIds(ids)
        insertEvent(correlationId, event, eventName, ids)
    }

/**
 * Stores [event] only if every id in [expected] still has exactly that version; otherwise stores
 * nothing and returns [ConcurrencyConflict] naming the ids whose version changed.
 *
 * Runs in its own READ COMMITTED transaction: per-id locks first, then a fresh recheck of the
 * versions, then the insert. Must not be called inside an outer transaction — an outer
 * REPEATABLE READ snapshot would predate the locks and hide concurrent commits from the recheck.
 */
fun <E : Event> dbStoreEventIf(
    correlationId: ULong,
    event: E,
    eventName: EventName,
    ids: List<Id>,
    expected: Versions
): Either<ConcurrencyConflict, EventEnvelope<E>> {
    check(TransactionManager.currentOrNull() == null) {
        "Conditional append must not run inside an outer transaction: the version recheck needs a fresh snapshot"
    }
    return transaction(Connection.TRANSACTION_READ_COMMITTED) {
        lockIds(ids + expected.keys)
        val stale = expected.filter { (id, version) -> dbVersionOf(id) != version }.keys.toSet()
        if (stale.isNotEmpty()) ConcurrencyConflict(stale).left()
        else insertEvent(correlationId, event, eventName, ids).right()
    }
}

/**
 * Serializes appends per id: `pg_advisory_xact_lock` on each id, in sorted key order (deadlock-free),
 * held until the transaction ends. With every append holding the locks of the ids it carries,
 * `seq` order equals commit order for any single id, which is what makes `max(seq)` per id a
 * gap-free version.
 */
private fun Transaction.lockIds(ids: Collection<Id>) {
    ids.map { lockKey(it) }.toSortedSet().forEach { key ->
        exec(
            "SELECT pg_advisory_xact_lock(hashtextextended(?::text, 0::bigint))",
            listOf(TextColumnType() to key)
        ) { }
    }
}

private fun lockKey(id: Id): String = "${id::class.simpleName}:${id.id}"

private fun <E : Event> Transaction.insertEvent(
    correlationId: ULong,
    event: E,
    eventName: EventName,
    ids: List<Id>
): EventEnvelope<E> {
    val sequence = DbEvents.insert {
        it[DbEvents.correlationId] = correlationId
        it[DbEvents.name] = eventName.value
        it[DbEvents.data] = Json.valueToNode(event)
        it[DbEvents.ids] = Json.idsToNode(ids)
        it[DbEvents.createdAt] = Instant.now()
    } get DbEvents.sequence

    return EventEnvelope(
        sequence = Seq(sequence),
        correlationId = correlationId,
        event = event,
        eventName = eventName,
    )
}
```

- [x] **Step 5: Run the `eddi-db` tests to verify they pass**

Run: `./gradlew :eddi-db:test`
Expected: BUILD SUCCESSFUL; `DbStoreEventIfTest` (7 tests) plus Task 1's tests pass.

- [x] **Step 6: Commit**

```bash
git add eddi-api/src/main/kotlin/dev/oblac/eddi/models_event.kt eddi-api/src/main/kotlin/dev/oblac/eddi/models_command.kt eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbStoreEvent.kt eddi-db/src/test/kotlin/dev/oblac/eddi/db/DbStoreEventIfTest.kt
git commit -m "Add conditional append with per-id advisory locks and version recheck

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: `EventStore.versionOf`, conditional `storeEvent`, `DbEventStore`

**Files:**
- Modify: `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStore.kt`
- Modify: `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreInbox.kt`
- Create: `eddi-db/src/test/kotlin/dev/oblac/eddi/db/DbEventStoreTest.kt`
- Modify: `eddi-db/src/main/kotlin/dev/oblac/eddi/db/DbEventStore.kt`

**Interfaces:**
- Consumes: `dbVersionOf` (Task 1), `dbStoreEventIf` (Task 2), `Versions`, `ConcurrencyConflict` (Task 2).
- Produces: `EventStore.versionOf(id: Id): Seq`; `EventStoreInbox.storeEvent(event: E, expected: Versions, correlationId: ULong = 0u): Either<ConcurrencyConflict, EventEnvelope<E>>` (the existing `storeEvent(event, correlationId = 0u): EventEnvelope<E>` stays).

- [x] **Step 1: Write the failing test**

`eddi-db/src/test/kotlin/dev/oblac/eddi/db/DbEventStoreTest.kt`:

```kotlin
package dev.oblac.eddi.db

import dev.oblac.eddi.ConcurrencyConflict
import dev.oblac.eddi.Seq
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DbEventStoreTest {

    private val store = DbEventStore()

    @BeforeEach
    fun reset() = TestDb.reset()

    @Test
    fun `versionOf reflects stored events`() {
        val id = PingId(UUID.randomUUID())
        assertEquals(Seq.ZERO, store.versionOf(id))

        val stored = store.storeEvent(Pinged(id))

        assertEquals(stored.sequence, store.versionOf(id))
    }

    @Test
    fun `conditional storeEvent appends on current versions and conflicts on stale ones`() {
        val id = PingId(UUID.randomUUID())

        val first = store.storeEvent(Pinged(id), expected = mapOf(id to Seq.ZERO))
        val stale = store.storeEvent(Pinged(id), expected = mapOf(id to Seq.ZERO))

        assertTrue(first.isRight(), first.toString())
        assertEquals(ConcurrencyConflict(setOf(id)), stale.leftOrNull())
        assertEquals(first.getOrNull()!!.sequence, store.versionOf(id))
    }
}
```

- [x] **Step 2: Run the test to verify it fails to compile**

Run: `./gradlew :eddi-db:test --tests 'dev.oblac.eddi.db.DbEventStoreTest'`
Expected: FAIL — `Unresolved reference: versionOf` and no matching `storeEvent(event, expected = …)`.

- [x] **Step 3: Extend the interfaces**

Replace the whole of `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStore.kt` with:

```kotlin
package dev.oblac.eddi

interface EventStore : EventStoreInbox, EventStoreRepo {

    /**
     * Version of [id]: the sequence of the latest event carrying it, [Seq.ZERO] if none.
     */
    fun versionOf(id: Id): Seq
}
```

Replace the whole of `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreInbox.kt` with:

```kotlin
package dev.oblac.eddi

import arrow.core.Either

interface EventStoreInbox {

    /**
     * Stores the given event associated with the provided correlation ID.
     * Returns an EventEnvelope containing metadata about the stored event.
     */
    fun <E : Event> storeEvent(event: E, correlationId: ULong = 0u): EventEnvelope<E>

    /**
     * Stores [event] only if every id in [expected] still has exactly that version; otherwise
     * stores nothing and returns [ConcurrencyConflict] naming the ids whose version changed.
     * Must not be called inside an outer transaction.
     */
    fun <E : Event> storeEvent(
        event: E,
        expected: Versions,
        correlationId: ULong = 0u
    ): Either<ConcurrencyConflict, EventEnvelope<E>>
}
```

- [x] **Step 4: Implement them in `DbEventStore`**

Replace the whole of `eddi-db/src/main/kotlin/dev/oblac/eddi/db/DbEventStore.kt` with:

```kotlin
package dev.oblac.eddi.db

import arrow.core.Either
import dev.oblac.eddi.*

class DbEventStore : EventStore {

    override fun <E : Event> storeEvent(event: E, correlationId: ULong): EventEnvelope<E> =
        dbStoreEvent(correlationId, event, Events.nameOf(event), event.ids())

    override fun <E : Event> storeEvent(
        event: E,
        expected: Versions,
        correlationId: ULong
    ): Either<ConcurrencyConflict, EventEnvelope<E>> =
        dbStoreEventIf(correlationId, event, Events.nameOf(event), event.ids(), expected)

    override fun versionOf(id: Id): Seq = dbVersionOf(id)

    private val eventProcessor = DbEventProcessor(processorId = 1L)

    fun startInbox(eventListener: EventListener) {
        eventProcessor.startInbox(eventListener)
    }

    override fun <T : Event> findEventById(eventName: EventName, id: Id): EventEnvelope<T>? =
        dbFindEventById(eventName, id) as EventEnvelope<T>?

    override fun <T : Event> findEventByMultipleIds(eventName: EventName, vararg ids: Id): EventEnvelope<T>? =
        dbFindEventByMultipleIds(eventName, *ids) as EventEnvelope<T>?

    override fun <T : Event> findEvents(name: EventName, dataFilters: Map<String, String>): List<EventEnvelope<T>> {
        return dbFindEventsByName(name.value, dataFilters) as List<EventEnvelope<T>>
    }
}
```

- [x] **Step 5: Run the `eddi-db` tests and compile everything**

Run: `./gradlew :eddi-db:test testClasses`
Expected: BUILD SUCCESSFUL; `DbEventStoreTest` (2 tests) passes and every module's main and test sources compile (the example still uses the unconditional path). Do not use `-x test` here — it would exclude `:eddi-db:test` too.

- [x] **Step 6: Commit**

```bash
git add eddi-api/src/main/kotlin/dev/oblac/eddi/EventStore.kt eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreInbox.kt eddi-db/src/main/kotlin/dev/oblac/eddi/db/DbEventStore.kt eddi-db/src/test/kotlin/dev/oblac/eddi/db/DbEventStoreTest.kt
git commit -m "Expose versionOf and conditional storeEvent on EventStore; implement in DbEventStore

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: `EventStoreSession`

**Files:**
- Create: `eddi-api/src/test/kotlin/dev/oblac/eddi/FakeEventStore.kt`
- Create: `eddi-api/src/test/kotlin/dev/oblac/eddi/EventStoreSessionTest.kt`
- Create: `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreSession.kt`
- Modify: `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreRepo.kt` (KDoc only)

**Interfaces:**
- Consumes: `EventStore.versionOf`, conditional `storeEvent` (Task 3), `Versions`, `ConcurrencyConflict` (Task 2).
- Produces: `class EventStoreSession(store: EventStore) : EventStoreRepo` with `val recorded: Versions` and `fun <E : Event> append(event: E, correlationId: ULong = 0u): Either<ConcurrencyConflict, EventEnvelope<E>>`; `fun EventStore.session(): EventStoreSession`; test fixtures `FakeEventStore`, `FakeId`, `FakeEvent`, `PingCommand`.

- [x] **Step 1: Write the fake store and the failing tests**

`eddi-api/src/test/kotlin/dev/oblac/eddi/FakeEventStore.kt`:

```kotlin
package dev.oblac.eddi

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.util.UUID

@JvmInline
value class FakeId(override val id: UUID) : Id

data class FakeEvent(val fake: FakeId) : Event {
    override fun ids() = listOf(fake)
}

object PingCommand : Command

/**
 * In-memory [EventStore] for session and effect tests: scripted versions, a call log to assert
 * call order, and a switch that makes the conditional append conflict.
 */
class FakeEventStore : EventStore {
    val calls = mutableListOf<String>()
    val versions = mutableMapOf<Id, Seq>()
    var lastExpected: Versions? = null
    var conflictOn: Set<Id>? = null
    private var nextSeq = 0L

    override fun versionOf(id: Id): Seq {
        calls += "versionOf:${id.id}"
        return versions[id] ?: Seq.ZERO
    }

    override fun <T : Event> findEventById(eventName: EventName, id: Id): EventEnvelope<T>? {
        calls += "findEventById:${id.id}"
        return null
    }

    override fun <T : Event> findEventByMultipleIds(eventName: EventName, vararg ids: Id): EventEnvelope<T>? {
        calls += "findEventByMultipleIds:${ids.joinToString(",") { it.id.toString() }}"
        return null
    }

    override fun <T : Event> findEvents(name: EventName, dataFilters: Map<String, String>): List<EventEnvelope<T>> {
        calls += "findEvents:${name.value}"
        return emptyList()
    }

    override fun <E : Event> storeEvent(event: E, correlationId: ULong): EventEnvelope<E> {
        calls += "storeEvent"
        return envelope(event, correlationId)
    }

    override fun <E : Event> storeEvent(
        event: E,
        expected: Versions,
        correlationId: ULong
    ): Either<ConcurrencyConflict, EventEnvelope<E>> {
        calls += "storeEvent:expected"
        lastExpected = expected
        conflictOn?.let { return ConcurrencyConflict(it).left() }
        return envelope(event, correlationId).right()
    }

    private fun <E : Event> envelope(event: E, correlationId: ULong) =
        EventEnvelope(Seq.of(++nextSeq), correlationId, event, EventName.of(event::class))
}
```

`eddi-api/src/test/kotlin/dev/oblac/eddi/EventStoreSessionTest.kt`:

```kotlin
package dev.oblac.eddi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class EventStoreSessionTest {

    private val store = FakeEventStore()
    private val session = store.session()
    private val id = FakeId(UUID.randomUUID())
    private val other = FakeId(UUID.randomUUID())

    @Test
    fun `findEventById records the version before delegating the read`() {
        store.versions[id] = Seq.of(7L)

        session.findEventById<FakeEvent>(id)

        assertEquals(listOf("versionOf:${id.id}", "findEventById:${id.id}"), store.calls)
        assertEquals(mapOf<Id, Seq>(id to Seq.of(7L)), session.recorded)
    }

    @Test
    fun `the first recorded version wins over later reads of the same id`() {
        store.versions[id] = Seq.of(7L)
        session.findEventById<FakeEvent>(id)
        store.versions[id] = Seq.of(9L)

        session.findEventById<FakeEvent>(id)

        assertEquals(mapOf<Id, Seq>(id to Seq.of(7L)), session.recorded)
        assertEquals(1, store.calls.count { it.startsWith("versionOf:") })
    }

    @Test
    fun `a read that finds nothing is recorded as ZERO`() {
        session.findEventById<FakeEvent>(id)

        assertEquals(mapOf<Id, Seq>(id to Seq.ZERO), session.recorded)
    }

    @Test
    fun `findEventByMultipleIds records every id before the read`() {
        store.versions[other] = Seq.of(3L)

        session.findEventByMultipleIds<FakeEvent>(id, other)

        assertEquals(
            listOf("versionOf:${id.id}", "versionOf:${other.id}", "findEventByMultipleIds:${id.id},${other.id}"),
            store.calls
        )
        assertEquals(mapOf<Id, Seq>(id to Seq.ZERO, other to Seq.of(3L)), session.recorded)
    }

    @Test
    fun `findEvents is outside the boundary and records nothing`() {
        session.findEvents<FakeEvent>(mapOf("label" to "x"))

        assertEquals(listOf("findEvents:FakeEvent"), store.calls)
        assertTrue(session.recorded.isEmpty())
    }

    @Test
    fun `append passes exactly the recorded versions as expected`() {
        store.versions[id] = Seq.of(7L)
        session.findEventById<FakeEvent>(id)

        val result = session.append(FakeEvent(id))

        assertTrue(result.isRight())
        assertEquals(mapOf<Id, Seq>(id to Seq.of(7L)), store.lastExpected)
    }

    @Test
    fun `append returns the store's conflict`() {
        session.findEventById<FakeEvent>(id)
        store.conflictOn = setOf(id)

        assertEquals(ConcurrencyConflict(setOf(id)), session.append(FakeEvent(id)).leftOrNull())
    }

    @Test
    fun `a session appends at most once`() {
        session.append(FakeEvent(id))

        assertThrows<IllegalStateException> { session.append(FakeEvent(id)) }
    }
}
```

- [x] **Step 2: Run the tests to verify they fail to compile**

Run: `./gradlew :eddi-api:test --tests 'dev.oblac.eddi.EventStoreSessionTest'`
Expected: FAIL — `Unresolved reference: session` / `EventStoreSession`.

- [x] **Step 3: Implement the session**

`eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreSession.kt`:

```kotlin
package dev.oblac.eddi

import arrow.core.Either

/**
 * A per-command view of an [EventStore]. Records the version of every id the command reads —
 * before reading it, first touch wins — and appends the resulting event only if none of those
 * versions changed in the meantime. Reads through [findEvents] carry no ids and are outside
 * this guarantee.
 *
 * One session per command invocation; a session appends at most once.
 */
class EventStoreSession(private val store: EventStore) : EventStoreRepo {

    private val versions = LinkedHashMap<Id, Seq>()
    private var appended = false

    /** Versions recorded so far, in first-touch order. */
    val recorded: Versions
        get() = versions.toMap()

    override fun <T : Event> findEventById(eventName: EventName, id: Id): EventEnvelope<T>? {
        touch(id)
        return store.findEventById(eventName, id)
    }

    override fun <T : Event> findEventByMultipleIds(eventName: EventName, vararg ids: Id): EventEnvelope<T>? {
        ids.forEach { touch(it) }
        return store.findEventByMultipleIds(eventName, *ids)
    }

    override fun <T : Event> findEvents(name: EventName, dataFilters: Map<String, String>): List<EventEnvelope<T>> =
        store.findEvents(name, dataFilters)

    /**
     * Appends [event] if every id read through this session still has the version it had when read.
     */
    fun <E : Event> append(event: E, correlationId: ULong = 0u): Either<ConcurrencyConflict, EventEnvelope<E>> {
        check(!appended) { "EventStoreSession already appended; create a new session per command" }
        appended = true
        return store.storeEvent(event, expected = recorded, correlationId = correlationId)
    }

    /** Records the current version of [id] on first touch — always before the read that follows. */
    private fun touch(id: Id) {
        versions.getOrPut(id) { store.versionOf(id) }
    }
}

/**
 * Opens a session: one per command invocation.
 */
fun EventStore.session(): EventStoreSession = EventStoreSession(this)
```

Then update the KDoc of `findEvents` in `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreRepo.kt` — change the interface method (line 10) to:

```kotlin
    /**
     * Events of [name] whose payload matches [dataFilters]. Carries no ids, so reads through it are
     * outside an [EventStoreSession]'s consistency boundary: a command must not base a decision on it.
     */
    fun <T: Event> findEvents(name: EventName, dataFilters: Map<String, String> = mapOf()): List<EventEnvelope<T>>
```

- [x] **Step 4: Run the `eddi-api` tests to verify they pass**

Run: `./gradlew :eddi-api:test`
Expected: BUILD SUCCESSFUL; `EventStoreSessionTest` (8 tests) and the existing `eddi-api` tests pass.

- [x] **Step 5: Commit**

```bash
git add eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreSession.kt eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreRepo.kt eddi-api/src/test/kotlin/dev/oblac/eddi/FakeEventStore.kt eddi-api/src/test/kotlin/dev/oblac/eddi/EventStoreSessionTest.kt
git commit -m "Add EventStoreSession: record read versions, append conditionally

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: `EventStore.commandHandler`

**Files:**
- Create: `eddi-api/src/test/kotlin/dev/oblac/eddi/SessionCommandHandlerTest.kt`
- Modify: `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreSession.kt`

**Interfaces:**
- Consumes: `EventStoreSession`, `session()` (Task 4), `FakeEventStore` fixtures (Task 4), `CommandHandler<R>`.
- Produces: `fun <E : Event> EventStore.commandHandler(decide: (EventStoreRepo, Command) -> Either<CommandError, E>): CommandHandler<EventEnvelope<E>>`.

- [x] **Step 1: Write the failing tests**

`eddi-api/src/test/kotlin/dev/oblac/eddi/SessionCommandHandlerTest.kt`:

```kotlin
package dev.oblac.eddi

import arrow.core.left
import arrow.core.right
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class SessionCommandHandlerTest {

    private val store = FakeEventStore()
    private val id = FakeId(UUID.randomUUID())

    private val reading = store.commandHandler<FakeEvent> { repo, _ ->
        repo.findEventById<FakeEvent>(id)
        FakeEvent(id).right()
    }

    @Test
    fun `decides through a session and appends through the same session`() {
        store.versions[id] = Seq.of(7L)

        val result = reading(PingCommand)

        assertTrue(result.isRight())
        assertEquals(mapOf<Id, Seq>(id to Seq.of(7L)), store.lastExpected)
    }

    @Test
    fun `a decision error is returned and nothing is appended`() {
        val failing = store.commandHandler<FakeEvent> { _, command -> UnknownCommandError(command).left() }

        assertEquals(UnknownCommandError(PingCommand), failing(PingCommand).leftOrNull())
        assertNull(store.lastExpected)
        assertFalse(store.calls.any { it.startsWith("storeEvent") })
    }

    @Test
    fun `an append conflict is returned as the command error`() {
        store.conflictOn = setOf(id)

        assertEquals(ConcurrencyConflict(setOf(id)), reading(PingCommand).leftOrNull())
    }

    @Test
    fun `each invocation gets a fresh session`() {
        reading(PingCommand)
        reading(PingCommand)

        // a reused session would record the version once and throw on the second append
        assertEquals(2, store.calls.count { it == "versionOf:${id.id}" })
        assertEquals(2, store.calls.count { it == "storeEvent:expected" })
    }
}
```

- [x] **Step 2: Run the tests to verify they fail to compile**

Run: `./gradlew :eddi-api:test --tests 'dev.oblac.eddi.SessionCommandHandlerTest'`
Expected: FAIL — `Unresolved reference: commandHandler` on `store`.

- [x] **Step 3: Implement the helper**

Append to `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreSession.kt` (and add `import arrow.core.flatMap` at the top):

```kotlin

/**
 * A [CommandHandler] that runs [decide] with a fresh session per command and appends the emitted
 * event through that same session, so the append is conditional on everything [decide] read.
 */
fun <E : Event> EventStore.commandHandler(
    decide: (EventStoreRepo, Command) -> Either<CommandError, E>
): CommandHandler<EventEnvelope<E>> = CommandHandler<EventEnvelope<E>> { command ->
    val session = session()
    decide(session, command).flatMap { session.append(it) }
}
```

- [x] **Step 4: Run the `eddi-api` tests to verify they pass**

Run: `./gradlew :eddi-api:test`
Expected: BUILD SUCCESSFUL; `SessionCommandHandlerTest` (4 tests) passes alongside the rest.

- [x] **Step 5: Commit**

```bash
git add eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreSession.kt eddi-api/src/test/kotlin/dev/oblac/eddi/SessionCommandHandlerTest.kt
git commit -m "Add EventStore.commandHandler: one session per command, append through it

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: `retryOnConflict`

**Files:**
- Create: `eddi-api/src/test/kotlin/dev/oblac/eddi/RetryOnConflictTest.kt`
- Modify: `eddi-api/src/main/kotlin/dev/oblac/eddi/Eddi.kt`

**Interfaces:**
- Consumes: `CommandHandler<R>`, `ConcurrencyConflict` (Task 2), `PingCommand`, `FakeId` (Task 4).
- Produces: `fun <R> CommandHandler<R>.retryOnConflict(times: Int = 3): CommandHandler<R>`.

- [x] **Step 1: Write the failing tests**

`eddi-api/src/test/kotlin/dev/oblac/eddi/RetryOnConflictTest.kt`:

```kotlin
package dev.oblac.eddi

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class RetryOnConflictTest {

    private val conflict = ConcurrencyConflict(setOf(FakeId(UUID.randomUUID())))

    /** A handler that returns [results] in order and counts its invocations. */
    private fun scripted(vararg results: Either<CommandError, String>): Pair<CommandHandler<String>, () -> Int> {
        var calls = 0
        val handler = CommandHandler<String> { _ -> results[calls++] }
        return handler to { calls }
    }

    @Test
    fun `re-runs on conflict and returns the first non-conflict result`() {
        val (handler, calls) = scripted(conflict.left(), conflict.left(), "ok".right())

        assertEquals("ok", handler.retryOnConflict(times = 3)(PingCommand).getOrNull())
        assertEquals(3, calls())
    }

    @Test
    fun `gives up after times re-runs and returns the conflict`() {
        val (handler, calls) = scripted(conflict.left(), conflict.left(), "ok".right())

        assertEquals(conflict, handler.retryOnConflict(times = 1)(PingCommand).leftOrNull())
        assertEquals(2, calls())
    }

    @Test
    fun `other errors pass through without re-running`() {
        val (handler, calls) = scripted(UnknownCommandError(PingCommand).left(), "ok".right())

        assertEquals(UnknownCommandError(PingCommand), handler.retryOnConflict()(PingCommand).leftOrNull())
        assertEquals(1, calls())
    }

    @Test
    fun `success passes through without re-running`() {
        val (handler, calls) = scripted("ok".right(), "again".right())

        assertEquals("ok", handler.retryOnConflict()(PingCommand).getOrNull())
        assertEquals(1, calls())
    }
}
```

- [x] **Step 2: Run the tests to verify they fail to compile**

Run: `./gradlew :eddi-api:test --tests 'dev.oblac.eddi.RetryOnConflictTest'`
Expected: FAIL — `Unresolved reference: retryOnConflict`.

- [x] **Step 3: Implement the effect**

Append to `eddi-api/src/main/kotlin/dev/oblac/eddi/Eddi.kt` (after `async`):

```kotlin

/**
 * Extension function to apply a retry effect to a CommandHandler.
 * Re-runs the command while it fails with [ConcurrencyConflict], at most [times] re-runs after the
 * first attempt. Every other result is returned as is.
 */
fun <R> CommandHandler<R>.retryOnConflict(times: Int = 3): CommandHandler<R> {
    val target = this
    return CommandHandler { command ->
        var result = target(command)
        var retry = 0
        while (retry < times && result.leftOrNull() is ConcurrencyConflict) {
            retry++
            println("🔁 Command ${command::class.simpleName} conflicted, retry $retry/$times")
            result = target(command)
        }
        result
    }
}
```

- [x] **Step 4: Run the `eddi-api` tests to verify they pass**

Run: `./gradlew :eddi-api:test`
Expected: BUILD SUCCESSFUL; `RetryOnConflictTest` (4 tests) passes alongside the rest.

- [x] **Step 5: Commit**

```bash
git add eddi-api/src/main/kotlin/dev/oblac/eddi/Eddi.kt eddi-api/src/test/kotlin/dev/oblac/eddi/RetryOnConflictTest.kt
git commit -m "Add retryOnConflict effect for command handlers

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 7: `EmailId` — email uniqueness inside the boundary

**Files:**
- Modify: `example-events/src/test/kotlin/dev/oblac/eddi/example/college/CommandProcessorTest.kt`
- Modify: `example-events/src/test/kotlin/dev/oblac/eddi/example/college/StubEventStoreRepo.kt`
- Modify: `example-events/src/main/kotlin/dev/oblac/eddi/example/college/registerStudent.kt`

**Interfaces:**
- Consumes: `EventStoreRepo.findEventById<E>(id)` (existing reified extension).
- Produces: `@JvmInline value class EmailId(override val id: UUID) : Id` with `EmailId.of(email: String)`; `StudentRegistered.ids()` = `[studentId, EmailId.of(email)]`.

- [x] **Step 1: Write the failing tests**

In `example-events/src/test/kotlin/dev/oblac/eddi/example/college/CommandProcessorTest.kt`, replace the test `StudentRegistered carries its own id` with:

```kotlin
    @Test
    fun `StudentRegistered carries its own id and the email id`() {
        val event = StudentRegistered(studentId, "Ada", "Lovelace", "ada@college.edu")

        assertEquals(listOf(studentId, EmailId.of("ada@college.edu")), event.ids())
    }
```

and add these tests (anywhere inside the class):

```kotlin
    @Test
    fun `EmailId is the same for the same email and different otherwise`() {
        assertEquals(EmailId.of("ada@college.edu"), EmailId.of("ada@college.edu"))
        assertNotEquals(EmailId.of("ada@college.edu"), EmailId.of("grace@college.edu"))
    }

    @Test
    fun `RegisterStudent fails when a student with the same email exists`() {
        val repo = StubEventStoreRepo(listOf(registered()))

        assertTrue(RegisterStudent("Ada", "Lovelace", "ada@college.edu")(repo).isLeft())
    }

    @Test
    fun `RegisterStudent succeeds when only a different email exists`() {
        val repo = StubEventStoreRepo(listOf(registered()))

        assertTrue(RegisterStudent("Grace", "Hopper", "grace@college.edu")(repo).isRight())
    }
```

- [x] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :example-events:test --tests 'dev.oblac.eddi.example.college.CommandProcessorTest'`
Expected: FAIL to compile — `Unresolved reference: EmailId`.

- [x] **Step 3: Add `EmailId`, carry it on the event, look it up in the guard**

In `example-events/src/main/kotlin/dev/oblac/eddi/example/college/registerStudent.kt`, after `StudentId` add:

```kotlin
/**
 * Value-derived id of an email address: the same email always yields the same [EmailId], so
 * "a student with this email exists" is an id lookup inside the consistency boundary.
 */
@JvmInline
value class EmailId(override val id: UUID) : Id {
    companion object {
        fun of(email: String) = EmailId(UUID.nameUUIDFromBytes("email:$email".toByteArray()))
    }
}
```

change `StudentRegistered.ids()` to:

```kotlin
    override fun ids() = listOf(studentId, EmailId.of(email))
```

and change `ensureUniqueEmail` to:

```kotlin
fun ensureUniqueEmail(es: EventStoreRepo) = commandProcessor<RegisterStudent> {
    ensure(
        es.findEventById<StudentRegistered>(EmailId.of(it.email)) == null
    ) { RegisterStudentError.StudentAlreadyExist }
}
```

- [x] **Step 4: Run the tests — expect one remaining failure, caused by the stub**

Run: `./gradlew :example-events:test --tests 'dev.oblac.eddi.example.college.CommandProcessorTest'`
Expected: FAIL — only `RegisterStudent succeeds when only a different email exists` fails: `StubEventStoreRepo.findEventById` matches by event name only, so the unrelated `StudentRegistered` is returned.

- [x] **Step 5: Make the stub match ids**

Replace the two lookups in `example-events/src/test/kotlin/dev/oblac/eddi/example/college/StubEventStoreRepo.kt` with:

```kotlin
    override fun <T : Event> findEventById(eventName: EventName, id: Id): EventEnvelope<T>? =
        events.lastOrNull { it.eventName == eventName && id in it.event.ids() } as EventEnvelope<T>?

    override fun <T : Event> findEventByMultipleIds(
        eventName: EventName,
        vararg ids: Id
    ): EventEnvelope<T>? =
        events.lastOrNull { it.eventName == eventName && it.event.ids().containsAll(ids.toList()) } as EventEnvelope<T>?
```

and update the class KDoc to:

```kotlin
/**
 * In-memory [EventStoreRepo] for processor tests. Id lookups match the event name and the ids the
 * event carries; [findEvents] matches by name only.
 */
```

- [x] **Step 6: Run the `example-events` tests to verify they pass**

Run: `./gradlew :example-events:test`
Expected: BUILD SUCCESSFUL; all `CommandProcessorTest` tests (including the three new ones) and `RegisterCollegeEventsTest` pass.

- [x] **Step 7: Commit**

```bash
git add example-events/src/main/kotlin/dev/oblac/eddi/example/college/registerStudent.kt example-events/src/test/kotlin/dev/oblac/eddi/example/college/StubEventStoreRepo.kt example-events/src/test/kotlin/dev/oblac/eddi/example/college/CommandProcessorTest.kt
git commit -m "Guard email uniqueness with a value-derived EmailId carried by StudentRegistered

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 8: `CourseNameId` — course-name uniqueness inside the boundary

**Files:**
- Modify: `example-events/src/test/kotlin/dev/oblac/eddi/example/college/CommandProcessorTest.kt`
- Modify: `example-events/src/main/kotlin/dev/oblac/eddi/example/college/publishCourse.kt`

**Interfaces:**
- Consumes: id-matching `StubEventStoreRepo` (Task 7).
- Produces: `@JvmInline value class CourseNameId(override val id: UUID) : Id` with `CourseNameId.of(courseName: String)`; `CoursePublished.ids()` = `[courseId, CourseNameId.of(courseName)]`.

- [x] **Step 1: Write the failing tests**

Add to `CommandProcessorTest`:

```kotlin
    private fun published() =
        envelope(CoursePublished(courseId, "Algebra", "Noether"))

    @Test
    fun `CoursePublished carries its own id and the course name id`() {
        val event = CoursePublished(courseId, "Algebra", "Noether")

        assertEquals(listOf(courseId, CourseNameId.of("Algebra")), event.ids())
    }

    @Test
    fun `PublishCourse fails when a course with the same name exists`() {
        val repo = StubEventStoreRepo(listOf(published()))

        assertTrue(PublishCourse("Algebra", "Noether")(repo).isLeft())
    }

    @Test
    fun `PublishCourse succeeds when only a different course name exists`() {
        val repo = StubEventStoreRepo(listOf(published()))

        assertTrue(PublishCourse("Topology", "Noether")(repo).isRight())
    }
```

- [x] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :example-events:test --tests 'dev.oblac.eddi.example.college.CommandProcessorTest'`
Expected: FAIL to compile — `Unresolved reference: CourseNameId`.

- [x] **Step 3: Add `CourseNameId`, carry it on the event, look it up in the guard**

In `example-events/src/main/kotlin/dev/oblac/eddi/example/college/publishCourse.kt`, after `CourseId` add:

```kotlin
/**
 * Value-derived id of a course name: the same name always yields the same [CourseNameId], so
 * "a course with this name exists" is an id lookup inside the consistency boundary.
 */
@JvmInline
value class CourseNameId(override val id: UUID) : Id {
    companion object {
        fun of(courseName: String) = CourseNameId(UUID.nameUUIDFromBytes("courseName:$courseName".toByteArray()))
    }
}
```

change `CoursePublished.ids()` to:

```kotlin
    override fun ids() = listOf(courseId, CourseNameId.of(courseName))
```

and change `ensureUniqueCourse` to:

```kotlin
fun ensureUniqueCourse(es: EventStoreRepo) = commandProcessor<PublishCourse> {
    ensure(
        es.findEventById<CoursePublished>(CourseNameId.of(it.courseName)) == null
    ) { PublishCourseError.CourseAlreadyExists }
}
```

- [x] **Step 4: Run the `example-events` tests to verify they pass**

Run: `./gradlew :example-events:test`
Expected: BUILD SUCCESSFUL; the three new tests pass with the rest.

- [x] **Step 5: Commit**

```bash
git add example-events/src/main/kotlin/dev/oblac/eddi/example/college/publishCourse.kt example-events/src/test/kotlin/dev/oblac/eddi/example/college/CommandProcessorTest.kt
git commit -m "Guard course-name uniqueness with a value-derived CourseNameId carried by CoursePublished

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 9: Wire the example, delete `TxCommandHandler`, verify concurrently

**Files:**
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/commandHandler.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/main.kt`
- Delete: `eddi-db/src/main/kotlin/dev/oblac/eddi/db/txCommandHandler.kt`

**Interfaces:**
- Consumes: `EventStore.commandHandler(decide)` (Task 5), `retryOnConflict()` (Task 6), `async()` (existing).
- Produces: `Main.launch: AsyncCommandHandler<EventEnvelope<Event>>` (same type as today; the API endpoints keep calling `Main.launch(command).fold(...)`).

- [x] **Step 1: Route commands through a session**

Replace the whole of `example/src/main/kotlin/dev/oblac/eddi/example/college/commandHandler.kt` with:

```kotlin
package dev.oblac.eddi.example.college

import arrow.core.Either
import dev.oblac.eddi.Event
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.UnknownCommandError
import dev.oblac.eddi.commandHandler

/**
 * Main command handler that routes commands to their respective handlers.
 * Each command decides against its own session and its event is appended through that session,
 * so the append fails with a conflict if anything the command read changed in the meantime.
 */
fun commandHandler(es: EventStore) = es.commandHandler<Event> { s, command ->
    when (command) {
        is RegisterStudent -> command(s)
        is UpdateStudent -> command(s)
        is PayTuition -> command(s)
        is PublishCourse -> command(s)
        is EnrollStudentInCourse -> command(s)
        else -> {
            println("Unknown command: $command")
            Either.Left(UnknownCommandError(command))
        }
    }
}
```

- [x] **Step 2: Replace `.tx()` with `.retryOnConflict()` in `main.kt`**

In `example/src/main/kotlin/dev/oblac/eddi/example/college/main.kt`, replace the import `import dev.oblac.eddi.db.tx` with `import dev.oblac.eddi.retryOnConflict`, and change `Main` to:

```kotlin
// Stupid singleton to hold app-wide instances
object Main {
    val es = DbEventStore()

    val launch = commandHandler(es).retryOnConflict().async()

}
```

- [x] **Step 3: Delete `TxCommandHandler`**

```bash
git rm eddi-db/src/main/kotlin/dev/oblac/eddi/db/txCommandHandler.kt
```

- [x] **Step 4: Build everything and run every test**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL — all modules compile (nothing else referenced `tx()`), and the `eddi-api`, `eddi-json`, `eddi-db`, `example-events` tests pass.

- [x] **Step 5: Manual concurrency check against the real app**

The compose database may hold `StudentRegistered` rows without `EmailId`; the spec mandates a reset.

```bash
just infra-reset
until docker exec eddi-postgres pg_isready -U eddi_user -d eddi >/dev/null 2>&1; do sleep 1; done

./gradlew -q :example:classes
java -cp "$(./gradlew -q :example:printClasspath)" dev.oblac.eddi.example.college.MainKt > build/app.log 2>&1 &
until curl -sf localhost:8080/ >/dev/null; do sleep 1; done

# 20 identical registrations at once — every request is accepted (202), the outcome is async
for i in $(seq 1 20); do
  curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:8080/api/students \
    -d '{"firstName":"Ada","lastName":"Lovelace"}' &
done; wait
sleep 3

docker exec eddi-postgres psql -U eddi_user -d eddi -tAc "select count(*) from eddi.events where name = 'StudentRegistered'"
docker exec eddi-postgres psql -U eddi_user -d eddi -tAc "select count(*) from college.student"
grep -c "Student with this email already exists" build/app.log

# projections are still alive after the storm: a different student lands in the read model
curl -s -o /dev/null -X POST localhost:8080/api/students -d '{"firstName":"Grace","lastName":"Hopper"}'
sleep 2
docker exec eddi-postgres psql -U eddi_user -d eddi -tAc "select count(*) from college.student"

pkill -f 'dev.oblac.eddi.example.college.MainKt'
```

Expected: all 20 `curl` lines print `202`; the first two counts are `1`; the grep count is `19` (losers that retried and then failed properly — `🔁 … conflicted, retry` lines may also appear in `build/app.log`); the final count is `2`.

- [x] **Step 6: Commit**

```bash
git add example/src/main/kotlin/dev/oblac/eddi/example/college/commandHandler.kt example/src/main/kotlin/dev/oblac/eddi/example/college/main.kt
git commit -m "Run commands through sessions with conflict retry; drop TxCommandHandler

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Plan Self-Review

**Spec coverage:** §1 framework core → Tasks 2 (types), 3 (interfaces), 4 (session rules 1–5), 5 (`EventStore.commandHandler`), 6 (`retryOnConflict`). §2 Postgres append (locks, READ COMMITTED, sorted keys, recheck, outer-transaction guard, unconditional path locks, `versionOf`, `idsContain`) → Tasks 1–3. §3 domain → Tasks 7–8. §4 wiring and `TxCommandHandler` deletion → Task 9. §5 error handling → Tasks 2, 4, 6, 9. §6 testing matrix → every task; manual check → Task 9 step 5. §7 reset → Task 9 step 5. Deviation noted inline: only `org.testcontainers:postgresql` is added (the `junit-jupiter` extension is unnecessary with the singleton container).

**Type consistency:** `Versions = Map<Id, Seq>`; `ConcurrencyConflict(stale: Set<Id>)`; `dbStoreEventIf(correlationId, event, eventName, ids, expected)`; `EventStoreInbox.storeEvent(event, expected, correlationId = 0u)`; `EventStore.versionOf(id)`; `EventStoreSession.append(event, correlationId = 0u)` / `recorded`; `EventStore.session()`; `EventStore.commandHandler<E>(decide: (EventStoreRepo, Command) -> Either<CommandError, E>)`; `CommandHandler<R>.retryOnConflict(times = 3)` — used with the same names and shapes in every task.
