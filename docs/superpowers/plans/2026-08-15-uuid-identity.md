# UUID Tag Identity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move `Tag` identity off the event-log sequence (`Seq`) and onto a caller-minted `UUID`, so events can carry their own identity in their payload.

**Architecture:** `Tag.id` becomes a `UUID` and `Ref` carries it instead of a `Seq`. Events that have a tag type mint their own id, carry it in the payload, and self-reference it via `EventMeta.refs()` — which replaces the primary-key-by-sequence lookup that identity-as-seq made possible. `Seq` survives untouched as the log position on `EventEnvelope`.

**Tech Stack:** Kotlin 2.2.0 (JVM toolchain 21), Gradle 8.14.3, Jackson 2.18.0, Exposed 0.56.0, PostgreSQL (jsonb + GIN), Arrow 2.2.0, JUnit Jupiter 5.12.1.

**Spec:** `docs/superpowers/specs/2026-08-15-uuid-identity-design.md`

## Global Constraints

- Module dependency order is `eddi-api` → `eddi-json` → `eddi-db` → `example-events` → `example`. Tasks follow this order; **the full `./gradlew build` is red from Task 2 until Task 6 completes.** Each task verifies only its own module, using the exact command given in its steps.
- `Seq` (`eddi-api/models_event.kt`) must not change. It stays the event-log position.
- UUIDs are minted by the caller (API layer), never inside a command processor or event default.
- Only events that have a `Tag` type mint an id: `StudentRegistered`, `CoursePublished`, `StudentUpdated`. `TuitionPaid` and `StudentEnrolledInCourse` stay id-free.
- The database is wiped, not migrated. `V1`/`V2` are edited in place; `just infra-reset` is mandatory before running the app.
- Commit after every task.

---

### Task 1: Test infrastructure

The repo has zero test sources today. `junit-jupiter-engine` is declared in the version catalog but unused by any module, and `junit-jupiter-api` (needed to *write* tests) is missing entirely.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `eddi-api/build.gradle.kts`
- Modify: `eddi-json/build.gradle.kts`
- Modify: `example-events/build.gradle.kts`
- Create: `eddi-json/src/test/kotlin/dev/oblac/eddi/json/HarnessTest.kt`

**Interfaces:**
- Consumes: nothing
- Produces: a `junit` bundle in the version catalog; `testImplementation(libs.bundles.junit)` wiring in `eddi-api`, `eddi-json`, `example-events`

- [ ] **Step 1: Add `junit-jupiter-api` and a `junit` bundle to the catalog**

Gradle 8.14.3 injects its own (older) `junit-platform-launcher`, which cannot discover
Jupiter 5.12.1 tests — it fails with *"OutputDirectoryProvider not available; probably due
to unaligned versions of the junit-platform-engine and junit-platform-launcher jars"*. An
aligned launcher must be added explicitly on the test runtime classpath.

In `gradle/libs.versions.toml`, add to `[versions]`:

```toml
junit-platform = "1.12.1"
```

add to `[libraries]` (next to the existing `junit-jupiter-engine` line):

```toml
junit-jupiter-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit-jupiter-engine" }
junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher", version.ref = "junit-platform" }
```

and add to `[bundles]`:

```toml
junit = ["junit-jupiter-api", "junit-jupiter-engine"]
```

- [ ] **Step 2: Wire test dependencies into the three modules**

`eddi-api/build.gradle.kts` — add to the `dependencies` block:

```kotlin
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
```

`eddi-json/build.gradle.kts` — add to the `dependencies` block:

```kotlin
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
```

`example-events/build.gradle.kts` — add a `dependencies` entry so the file reads:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":eddi-api"))
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
}
```

Note: `eddi-json` already declares `tasks.test { useJUnitPlatform() }`, and the root `build.gradle.kts` applies `useJUnitPlatform()` to every subproject's `test` task, so no extra test-task config is needed.

- [ ] **Step 3: Write a smoke test proving the harness runs**

Create `eddi-json/src/test/kotlin/dev/oblac/eddi/json/HarnessTest.kt`:

```kotlin
package dev.oblac.eddi.json

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HarnessTest {
    @Test
    fun `test harness runs`() {
        assertEquals(2, 1 + 1)
    }
}
```

- [ ] **Step 4: Run the test and confirm it passes**

Run: `./gradlew :eddi-json:test`
Expected: BUILD SUCCESSFUL, and `:eddi-json:test` is no longer reported as `NO-SOURCE`.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml eddi-api/build.gradle.kts eddi-json/build.gradle.kts example-events/build.gradle.kts eddi-json/src/test
git commit -m "test: add JUnit test infrastructure"
```

---

### Task 2: `eddi-api` — Tag and Ref become UUID-based

**Files:**
- Modify: `eddi-api/src/main/kotlin/dev/oblac/eddi/models_tag.kt`
- Modify: `eddi-api/src/main/kotlin/dev/oblac/eddi/models_meta.kt:27-31` (`refOf`)
- Modify: `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreRepo.kt` (remove `findEvent`)
- Create: `eddi-api/src/test/kotlin/dev/oblac/eddi/EventsRefOfTest.kt`

**Interfaces:**
- Consumes: JUnit wiring from Task 1
- Produces: `Tag<out Event> { val id: UUID }`; `Ref(name: EventName, id: UUID)`; `Events.refOf(tag): Ref`. `EventStoreRepo.findEvent(seq, name)` no longer exists.

- [ ] **Step 1: Write the failing test**

Create `eddi-api/src/test/kotlin/dev/oblac/eddi/EventsRefOfTest.kt`:

```kotlin
package dev.oblac.eddi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

private data class Registered(val label: String) : Event

@JvmInline
private value class RegisteredId(override val id: UUID) : Tag<Registered>

class EventsRefOfTest {
    @Test
    fun `refOf builds a Ref from the tag's uuid`() {
        val name = EventName("Registered")
        Events.register(RegisteredId::class, name)

        val uuid = UUID.fromString("3f2a0000-0000-0000-0000-000000000001")

        assertEquals(Ref(name, uuid), Events.refOf(RegisteredId(uuid)))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :eddi-api:test`
Expected: FAIL — compilation error, `Tag` has no member `id` (it still declares `seq: Seq`).

- [ ] **Step 3: Change `Tag` and `Ref`**

Replace the whole of `eddi-api/src/main/kotlin/dev/oblac/eddi/models_tag.kt`:

```kotlin
package dev.oblac.eddi

import java.util.UUID

/**
 * Event tags.
 */
interface Tag<out Event> {
    val id: UUID
}

/**
 * Untyped reference to an event.
 */
data class Ref(
    val name: EventName,
    val id: UUID
)
```

- [ ] **Step 4: Update `Events.refOf`**

In `eddi-api/src/main/kotlin/dev/oblac/eddi/models_meta.kt`, replace the `refOf` function:

```kotlin
    fun refOf(tag: Tag<Event>): Ref {
        val name = klassToName[tag::class] ?: error("Tag ${tag::class.simpleName} is not registered")
        return Ref(name, tag.id)
    }
```

- [ ] **Step 5: Remove the by-sequence lookup from the repo interface**

Identity is no longer a sequence, so this lookup has no meaning and (after Task 5) no callers. In `eddi-api/src/main/kotlin/dev/oblac/eddi/EventStoreRepo.kt`, delete these lines:

```kotlin
    /**
     * Finds a specific event by its sequence ID and name.
     */
    fun <T: Event> findEvent(seq: Seq, name: EventName): EventEnvelope<T>?
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :eddi-api:test`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add eddi-api
git commit -m "feat(api): make Tag identity a UUID instead of a Seq"
```

---

### Task 3: `eddi-json` — serialize refs and tags as UUID strings

`RefSerializer` writes a number today; it must write a string. The reflection-based event deserializer at `Json.kt:65-148` extracts tag values with `asLong()`; it must use `UUID.fromString`.

**Files:**
- Modify: `eddi-json/src/main/kotlin/dev/oblac/eddi/json/Json.kt:17-34` (Ref serializer/deserializer)
- Modify: `eddi-json/src/main/kotlin/dev/oblac/eddi/json/Json.kt:127-136` (tag branch of `fromNode`)
- Create: `eddi-json/src/test/kotlin/dev/oblac/eddi/json/TagJsonTest.kt`

**Interfaces:**
- Consumes: `Tag.id: UUID` and `Ref(name, id)` from Task 2
- Produces: tags jsonb wire format `[{"EventName": "<uuid>"}]`; `Json.fromNode` reconstructs UUID-backed tag properties

- [ ] **Step 1: Write the failing tests**

Create `eddi-json/src/test/kotlin/dev/oblac/eddi/json/TagJsonTest.kt`. The fixtures are local to the test so `eddi-json` needs no dependency on the domain modules. `FixtureEvent` deliberately covers all three shapes the deserializer branches on: a non-null tag (flattened to `UUID` at the JVM level), a nullable tag (boxed), and a defaulted parameter.

```kotlin
package dev.oblac.eddi.json

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventName
import dev.oblac.eddi.Ref
import dev.oblac.eddi.Tag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@JvmInline
value class FixtureTag(override val id: UUID) : Tag<FixtureEvent>

data class FixtureEvent(
    val ownId: FixtureTag,
    val parent: FixtureTag?,
    val label: String,
    val at: Instant = Instant.EPOCH
) : Event

class TagJsonTest {

    @Test
    fun `Ref serializes as a uuid string keyed by event name`() {
        val uuid = UUID.fromString("3f2a0000-0000-0000-0000-000000000001")
        val ref = Ref(EventName("FixtureEvent"), uuid)

        assertEquals("""[{"FixtureEvent":"$uuid"}]""", Json.toJson(arrayOf(ref)))
    }

    @Test
    fun `Ref round-trips through JSON`() {
        val ref = Ref(EventName("FixtureEvent"), UUID.randomUUID())

        val back = Json.fromJson<Array<Ref>>(Json.toJson(arrayOf(ref)))

        assertEquals(ref, back.single())
    }

    @Test
    fun `event with a non-null and a non-null nullable tag round-trips`() {
        val event = FixtureEvent(FixtureTag(UUID.randomUUID()), FixtureTag(UUID.randomUUID()), "hello")

        val back = Json.fromNode(Json.valueToNode(event), FixtureEvent::class.java)

        assertEquals(event, back)
    }

    @Test
    fun `event with a null tag property round-trips`() {
        val event = FixtureEvent(FixtureTag(UUID.randomUUID()), null, "hello")

        val back = Json.fromNode(Json.valueToNode(event), FixtureEvent::class.java)

        assertEquals(event, back)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :eddi-json:test`
Expected: FAIL — `Ref` no longer has `seq`, so `Json.kt` does not compile.

- [ ] **Step 3: Update the Ref serializer and deserializer**

In `eddi-json/src/main/kotlin/dev/oblac/eddi/json/Json.kt`, replace both classes:

```kotlin
private class RefSerializer : JsonSerializer<Ref>() {
    override fun serialize(value: Ref, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField(value.name.value, value.id.toString())
        gen.writeEndObject()
    }
}

private class RefDeserializer : JsonDeserializer<Ref>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Ref {
        val node = p.codec.readTree<JsonNode>(p)
        val field = node.fields().next()
        return Ref(
            name = EventName(field.key),
            id = UUID.fromString(field.value.asText())
        )
    }
}
```

Add `import java.util.UUID` to the file's imports. Leave `SeqSerializer` and `SeqDeserializer` untouched — `Seq` still exists and is still serialized as a number.

- [ ] **Step 4: Update the tag branch of `fromNode`**

In the same file, replace the `else if (Tag::class.java.isAssignableFrom(paramClass.java))` branch:

```kotlin
            } else if (Tag::class.java.isAssignableFrom(paramClass.java)) {
                val uuid = UUID.fromString(jsonValue.asText())
                if (jvmType == UUID::class.java) {
                    // Non-nullable value class, flattened to UUID at JVM level
                    jvmArgs.add(uuid)
                } else {
                    // Nullable value class, boxed at JVM level
                    val tagCtor = paramClass.java.declaredConstructors.first { it.parameterCount == 1 }
                    tagCtor.isAccessible = true
                    jvmArgs.add(tagCtor.newInstance(uuid))
                }
            } else {
```

Also update the KDoc on `fromNode` — the phrase "(Tag wraps Seq wraps ULong)" is now wrong. Replace that parenthetical with "(Tag wraps UUID)".

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :eddi-json:test`
Expected: PASS — all four tests green.

- [ ] **Step 6: Commit**

```bash
git add eddi-json
git commit -m "feat(json): serialize tag refs as uuid strings"
```

---

### Task 4: `eddi-db` — jsonb needle takes a quoted uuid

**Files:**
- Modify: `eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbFindEventByTag.kt` (two needle sites)
- Delete: `eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbFindEventBySeqAndName.kt`
- Modify: `eddi-db/src/main/kotlin/dev/oblac/eddi/db/DbEventStore.kt:37-39` (remove `findEvent` override)

**Interfaces:**
- Consumes: `Ref.id: UUID` from Task 2
- Produces: tag queries matching `tags @> '[{"Name": "<uuid>"}]'::jsonb`

This task has no unit test — the queries need a live PostgreSQL instance. It is covered by the end-to-end verification in Task 7.

- [ ] **Step 1: Update the needle in `dbFindEventByTag`**

In `eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbFindEventByTag.kt`, inside `fun dbFindEventByTag(...)`, replace these two lines:

```kotlin
            val value = ref.seq.value
            val needle = """[{"$key": $value}]"""
```

with:

```kotlin
            val value = ref.id
            val needle = """[{"$key": "$value"}]"""
```

- [ ] **Step 2: Update the needle in `dbFindEventByMultipleTags`**

Further down the same file, inside `fun dbFindEventByMultipleTags(...)`, make the identical replacement:

```kotlin
                    val value = ref.id
                    val needle = """[{"$key": "$value"}]"""
```

Leave `dbFindLastEventByTag.kt` alone. It filters only on `ref.name` and `sequence` and never reads the ref's value — a pre-existing quirk in a function with no callers, documented in the spec as out of scope.

- [ ] **Step 3: Delete the by-sequence lookup**

```bash
git rm eddi-db/src/main/kotlin/dev/oblac/eddi/db/dbFindEventBySeqAndName.kt
```

Then in `eddi-db/src/main/kotlin/dev/oblac/eddi/db/DbEventStore.kt`, delete the now-orphaned override:

```kotlin
    override fun <T: Event> findEvent(seq: Seq, name: EventName): EventEnvelope<T>? {
        return dbFindEventBySeqAndName(seq, name.value) as EventEnvelope<T>?
    }
```

- [ ] **Step 4: Verify the module compiles**

Run: `./gradlew :eddi-db:build`
Expected: BUILD SUCCESSFUL. Pre-existing unchecked-cast warnings in `DbEventStore.kt` are expected and unrelated.

- [ ] **Step 5: Commit**

```bash
git add eddi-db
git commit -m "feat(db): match tags by uuid in jsonb containment queries"
```

---

### Task 5: `example-events` — domain tags, commands, and self-tagging

Every tag becomes UUID-backed, the three tagged events carry their own id, commands accept ids from the caller, and each `refs()` self-references so the `ensure*Exists` guards can find events by tag instead of by sequence.

**Files:**
- Modify: `example-events/src/main/kotlin/dev/oblac/eddi/example/college/registerStudent.kt`
- Modify: `example-events/src/main/kotlin/dev/oblac/eddi/example/college/publishCourse.kt`
- Modify: `example-events/src/main/kotlin/dev/oblac/eddi/example/college/payTuition.kt`
- Modify: `example-events/src/main/kotlin/dev/oblac/eddi/example/college/updateStudent.kt`
- Modify: `example-events/src/main/kotlin/dev/oblac/eddi/example/college/enrollStudentInCourse.kt`
- Create: `example-events/src/test/kotlin/dev/oblac/eddi/example/college/StubEventStoreRepo.kt`
- Create: `example-events/src/test/kotlin/dev/oblac/eddi/example/college/CommandProcessorTest.kt`

**Interfaces:**
- Consumes: `Tag.id: UUID`, `Ref(name, id)` from Task 2; `EventStoreRepo` without `findEvent` from Task 2
- Produces:
  - `StudentId(id: UUID) : Tag<StudentRegistered>`
  - `CoursePublishedTag(id: UUID) : Tag<CoursePublished>`
  - `StudentUpdatedTag(id: UUID) : Tag<StudentUpdated>`
  - `RegisterStudent(studentId, firstName, lastName, email)`
  - `PublishCourse(courseId, courseName, instructor)`
  - `UpdateStudent(updateId, student, firstName, lastName)`
  - `PayTuition(student)` and `EnrollStudentInCourse(student, course)` — shapes unchanged
  - `StudentRegistered(studentId, …)`, `CoursePublished(courseId, …)`, `StudentUpdated(updateId, student, last, …)`
  - `EventEnvelope<T>.tag()` for the three tagged events, now reading the payload

- [ ] **Step 1: Write the stub repo**

Create `example-events/src/test/kotlin/dev/oblac/eddi/example/college/StubEventStoreRepo.kt`:

```kotlin
package dev.oblac.eddi.example.college

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventName
import dev.oblac.eddi.EventStoreRepo
import dev.oblac.eddi.Seq
import dev.oblac.eddi.Tag
import java.time.Instant

/**
 * In-memory [EventStoreRepo] for processor tests. Events are matched by name only,
 * which is enough for the guards under test.
 */
@Suppress("UNCHECKED_CAST")
class StubEventStoreRepo(
    private val events: List<EventEnvelope<out Event>> = emptyList()
) : EventStoreRepo {

    override fun <T : Event> findLastEventByTagBefore(lastEvent: Seq, tagToFind: Tag<T>): EventEnvelope<T>? = null

    override fun <T : Event> findEventByTag(eventName: EventName, tagToFind: Tag<T>): EventEnvelope<T>? =
        events.lastOrNull { it.eventName == eventName } as EventEnvelope<T>?

    override fun <T : Event> findEventByMultipleTags(
        eventName: EventName,
        vararg tagsToFind: Tag<Event>
    ): EventEnvelope<T>? =
        events.lastOrNull { it.eventName == eventName } as EventEnvelope<T>?

    override fun <T : Event> findEvents(name: EventName, dataFilters: Map<String, String>): List<EventEnvelope<T>> =
        events.filter { it.eventName == name } as List<EventEnvelope<T>>

    companion object {
        fun <E : Event> envelope(event: E, name: EventName, seq: Long = 1L): EventEnvelope<E> =
            EventEnvelope(Seq.of(seq), 0u, event, name, Instant.EPOCH)
    }
}
```

- [ ] **Step 2: Write the failing processor tests**

Create `example-events/src/test/kotlin/dev/oblac/eddi/example/college/CommandProcessorTest.kt`:

```kotlin
package dev.oblac.eddi.example.college

import dev.oblac.eddi.Ref
import dev.oblac.eddi.example.college.StubEventStoreRepo.Companion.envelope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class CommandProcessorTest {

    private val studentId = StudentId(UUID.randomUUID())
    private val courseId = CoursePublishedTag(UUID.randomUUID())

    private fun registered() =
        envelope(
            StudentRegistered(studentId, "Ada", "Lovelace", "ada@college.edu"),
            StudentRegisteredEvent.NAME
        )

    @Test
    fun `RegisterStudent emits an event carrying the supplied id`() {
        val cmd = RegisterStudent(studentId, "Ada", "Lovelace", "ada@college.edu")

        val event = cmd(StubEventStoreRepo()).getOrNull()!!

        assertEquals(studentId, event.studentId)
    }

    @Test
    fun `StudentRegistered self-tags with its own id`() {
        val event = StudentRegistered(studentId, "Ada", "Lovelace", "ada@college.edu")

        val refs = StudentRegisteredEvent.refs(event).toList()

        assertEquals(listOf(Ref(StudentRegisteredEvent.NAME, studentId.id)), refs)
    }

    @Test
    fun `UpdateStudent derives a null last when there is no previous update`() {
        val updateId = StudentUpdatedTag(UUID.randomUUID())
        val repo = StubEventStoreRepo(listOf(registered()))

        val event = UpdateStudent(updateId, studentId, "Ada", null)(repo).getOrNull()!!

        assertEquals(updateId, event.updateId)
        assertNull(event.last)
    }

    @Test
    fun `UpdateStudent chains last to the previous update's id`() {
        val previousId = StudentUpdatedTag(UUID.randomUUID())
        val updateId = StudentUpdatedTag(UUID.randomUUID())
        val repo = StubEventStoreRepo(
            listOf(
                registered(),
                envelope(
                    StudentUpdated(previousId, studentId, null, "Ada", null),
                    StudentUpdatedEvent.NAME,
                    seq = 2L
                )
            )
        )

        val event = UpdateStudent(updateId, studentId, "Grace", null)(repo).getOrNull()!!

        assertEquals(previousId, event.last)
        assertEquals(
            listOf(
                Ref(StudentUpdatedEvent.NAME, updateId.id),
                Ref(StudentRegisteredEvent.NAME, studentId.id),
                Ref(StudentUpdatedEvent.NAME, previousId.id)
            ),
            StudentUpdatedEvent.refs(event).toList()
        )
    }

    @Test
    fun `UpdateStudent fails when the student does not exist`() {
        val cmd = UpdateStudent(StudentUpdatedTag(UUID.randomUUID()), studentId, "Ada", null)

        val result = cmd(StubEventStoreRepo())

        assertTrue(result.isLeft())
    }

    @Test
    fun `PayTuition fails when the student does not exist`() {
        assertTrue(PayTuition(studentId)(StubEventStoreRepo()).isLeft())
    }

    @Test
    fun `StudentEnrolledInCourse refs both the student and the course`() {
        val event = StudentEnrolledInCourse(studentId, courseId)

        assertEquals(
            listOf(
                Ref(StudentRegisteredEvent.NAME, studentId.id),
                Ref(CoursePublishedEvent.NAME, courseId.id)
            ),
            StudentEnrolledInCourseEvent.refs(event).toList()
        )
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :example-events:test`
Expected: FAIL — compilation errors; `StudentId` still takes a `Seq`, `RegisterStudent` has no `studentId` parameter.

- [ ] **Step 4: Rewrite `registerStudent.kt`**

```kotlin
package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import dev.oblac.eddi.*
import java.time.Instant
import java.util.UUID

data class RegisterStudent(
    val studentId: StudentId,
    val firstName: String,
    val lastName: String,
    val email: String
) : Command

@JvmInline
value class StudentId(override val id: UUID) : Tag<StudentRegistered>

data class StudentRegistered(
    val studentId: StudentId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant = Instant.now()
) : Event

sealed interface RegisterStudentError : CommandError {
    data object StudentAlreadyExist : RegisterStudentError {
        override fun toString(): String = "Student with this email already exists"
    }
}

fun ensureUniqueEmail(es: EventStoreRepo) = commandProcessor<RegisterStudent> {
    ensure(
        es.findEvents<StudentRegistered>(
            StudentRegisteredEvent.NAME,
            mapOf("email" to it.email)
        ).isEmpty()
    ) { RegisterStudentError.StudentAlreadyExist }
}


operator fun RegisterStudent.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureUniqueEmail(es)
        emit { StudentRegistered(studentId, firstName, lastName, email) }
    }

/**
 * Meta companion class for [StudentRegistered].
 */
object StudentRegisteredEvent : EventMeta<StudentRegistered> {

    override val CLASS = StudentRegistered::class
    override val NAME = EventName.of(CLASS)

    override fun refs(event: StudentRegistered): Array<Ref> = arrayOf(
        Ref(NAME, event.studentId.id)
    )
}

fun EventEnvelope<StudentRegistered>.tag() = event.studentId
```

- [ ] **Step 5: Rewrite `publishCourse.kt`**

```kotlin
package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import dev.oblac.eddi.*
import java.time.Instant
import java.util.UUID

data class PublishCourse(
    val courseId: CoursePublishedTag,
    val courseName: String,
    val instructor: String,
) : Command

@JvmInline
value class CoursePublishedTag(override val id: UUID) : Tag<CoursePublished>

data class CoursePublished(
    val courseId: CoursePublishedTag,
    val courseName: String,
    val instructor: String,
    val publishAt: Instant = Instant.now()
) : Event

sealed interface PublishCourseError : CommandError {
    object CourseAlreadyExists : PublishCourseError
}

fun ensureUniqueCourse(es: EventStoreRepo) = commandProcessor<PublishCourse> {
    ensure(
        es.findEvents<CoursePublished>(
            CoursePublishedEvent.NAME,
            mapOf("courseName" to it.courseName)
        ).isEmpty()
    ) { PublishCourseError.CourseAlreadyExists }
}

operator fun PublishCourse.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureUniqueCourse(es)
        emit { CoursePublished(courseId, courseName, instructor) }
    }

/**
 * Meta companion class for [CoursePublished].
 */
object CoursePublishedEvent : EventMeta<CoursePublished> {

    override val CLASS = CoursePublished::class
    override val NAME = EventName.of(CLASS)

    override fun refs(event: CoursePublished): Array<Ref> = arrayOf(
        Ref(NAME, event.courseId.id)
    )
}

fun EventEnvelope<CoursePublished>.tag() = event.courseId
```

- [ ] **Step 6: Rewrite `payTuition.kt`**

The guards swap `findEvent(seq, NAME)` for `findEventByTag(NAME, tag)`, and `ensureTuitionNotAlreadyPaid` no longer needs to rebuild the tag.

```kotlin
package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import dev.oblac.eddi.*
import java.time.Instant

data class PayTuition(
    val student: StudentId,
) : Command

data class TuitionPaid(
    val student: StudentId,
    val paidAt: Instant = Instant.now(),
) : Event

sealed interface PayTuitionError : CommandError {
    object StudentNotFound : PayTuitionError
    object TuitionAlreadyPaid : PayTuitionError
}

fun ensurePayTuitionStudentExists(es: EventStoreRepo) = commandProcessor<PayTuition> {
    ensureNotNull(
        es.findEventByTag<StudentRegistered>(
            StudentRegisteredEvent.NAME,
            it.student
        )
    ) { PayTuitionError.StudentNotFound }
}

fun ensureTuitionNotAlreadyPaid(es: EventStoreRepo) = commandProcessor<PayTuition> {
    ensure(
        es.findEventByTag(
            TuitionPaidEvent.NAME,
            it.student
        ) == null
    ) { PayTuitionError.TuitionAlreadyPaid }
}

operator fun PayTuition.invoke(es: EventStoreRepo) =
    process(this) {
        +ensurePayTuitionStudentExists(es)
        +ensureTuitionNotAlreadyPaid(es)
        emit { TuitionPaid(student) }
    }

/**
 * Meta companion class for [TuitionPaid].
 */
object TuitionPaidEvent : EventMeta<TuitionPaid> {

    override val CLASS = TuitionPaid::class
    override val NAME = EventName.of(CLASS)

    override fun refs(event: TuitionPaid): Array<Ref> = arrayOf(
        Ref(StudentRegisteredEvent.NAME, event.student.id)
    )
}
```

- [ ] **Step 7: Rewrite `updateStudent.kt`**

`last` is still derived inside `emit`, but now reads the previous update's `updateId` from the payload via `tag()` rather than from the envelope sequence.

```kotlin
package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import dev.oblac.eddi.*
import java.time.Instant
import java.util.UUID

data class UpdateStudent(
    val updateId: StudentUpdatedTag,
    val student: StudentId,
    val firstName: String?,
    val lastName: String?
) : Command

@JvmInline
value class StudentUpdatedTag(override val id: UUID) : Tag<StudentUpdated>

data class StudentUpdated(
    val updateId: StudentUpdatedTag,
    val student: StudentId,
    val last: StudentUpdatedTag?,   // there may be no previous update, so this is nullable
    val firstName: String?,
    val lastName: String?,
    val updatedAt: Instant = Instant.now()
) : Event

sealed interface UpdateStudentError : CommandError {
    data object NothingToUpdate : UpdateStudentError {
        override fun toString(): String = "No fields to update"
    }

    data object StudentNotFound : UpdateStudentError {
        override fun toString(): String = "Student not found"
    }
}

fun ensureStudentExists(es: EventStoreRepo) = commandProcessor<UpdateStudent> {
    ensureNotNull(
        es.findEventByTag<StudentRegistered>(
            StudentRegisteredEvent.NAME,
            it.student
        )
    ) { UpdateStudentError.StudentNotFound }
}

fun ensureHasUpdateFields() = commandProcessor<UpdateStudent> {
    ensure(it.firstName != null || it.lastName != null)
    { UpdateStudentError.NothingToUpdate }
}

operator fun UpdateStudent.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureStudentExists(es)
        +ensureHasUpdateFields()
        emit {
            // the previous update of this student, so updates form a chain
            val last = es.findEventByMultipleTags<StudentUpdated>(
                StudentUpdatedEvent.NAME,
                student
            )?.tag()
            StudentUpdated(updateId, student, last, firstName, lastName)
        }
    }

/**
 * Meta companion class for [StudentUpdated].
 */
object StudentUpdatedEvent : EventMeta<StudentUpdated> {

    override val CLASS = StudentUpdated::class
    override val NAME = EventName.of(CLASS)

    override fun refs(event: StudentUpdated): Array<Ref> = listOfNotNull(
        Ref(NAME, event.updateId.id),
        Ref(StudentRegisteredEvent.NAME, event.student.id),
        event.last?.let { Ref(NAME, it.id) }
    ).toTypedArray()
}

fun EventEnvelope<StudentUpdated>.tag() = event.updateId
```

- [ ] **Step 8: Rewrite the guards in `enrollStudentInCourse.kt`**

Only the four guard functions and the meta object change; the command, event, and error declarations keep their current shape (they already use `StudentId` / `CoursePublishedTag`). Replace from `fun ensureEnrollStudentExists` to the end of the file:

```kotlin
fun ensureEnrollStudentExists(es: EventStoreRepo) = commandProcessor<EnrollStudentInCourse> {
    ensureNotNull(
        es.findEventByTag<StudentRegistered>(
            StudentRegisteredEvent.NAME,
            it.student
        )
    ) { EnrollStudentInCourseError.StudentNotFound(it.student) }
}

fun ensureCourseExists(es: EventStoreRepo) = commandProcessor<EnrollStudentInCourse> {
    ensureNotNull(
        es.findEventByTag<CoursePublished>(
            CoursePublishedEvent.NAME,
            it.course
        )
    ) { EnrollStudentInCourseError.CourseNotFound(it.course) }
}


fun ensureNotAlreadyEnrolled(es: EventStoreRepo) = commandProcessor<EnrollStudentInCourse> {
    ensure(
        es.findEventByMultipleTags<StudentEnrolledInCourse>(
            StudentEnrolledInCourseEvent.NAME,
            it.student,
            it.course
        ) == null
    ) { EnrollStudentInCourseError.AlreadyEnrolled(it.student) }
}


fun ensureTuitionPaid(es: EventStoreRepo) = commandProcessor<EnrollStudentInCourse> {
    ensureNotNull(
        es.findEventByTag(
            TuitionPaidEvent.NAME,
            it.student
        )
    ) { EnrollStudentInCourseError.TuitionNotPaid(it.student) }
}


operator fun EnrollStudentInCourse.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureEnrollStudentExists(es)
        +ensureCourseExists(es)
        +ensureNotAlreadyEnrolled(es)
        +ensureTuitionPaid(es)
        emit { StudentEnrolledInCourse(student, course) }
    }

/**
 * Meta companion class for [StudentEnrolledInCourse].
 */
object StudentEnrolledInCourseEvent : EventMeta<StudentEnrolledInCourse> {

    override val CLASS = StudentEnrolledInCourse::class
    override val NAME = EventName.of(CLASS)

    override fun refs(event: StudentEnrolledInCourse): Array<Ref> = arrayOf(
        Ref(StudentRegisteredEvent.NAME, event.student.id),
        Ref(CoursePublishedEvent.NAME, event.course.id)
    )
}
```

- [ ] **Step 9: Run the tests to verify they pass**

Run: `./gradlew :example-events:test`
Expected: PASS — all seven tests green. `EventsRegistry.kt` needs no change; it already references `StudentId` and registers by class.

- [ ] **Step 10: Commit**

```bash
git add example-events
git commit -m "feat(college): give tagged events uuid identity in their payload"
```

---

### Task 6: `example` — schema, projections, API, UI

**Files:**
- Modify: `example/src/main/resources/db/migration/college/V1__Create_project_tables.sql`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/projection/db/StudentTable.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/projection/db/CourseTable.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/projection/dbInsertStudent.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/projection/dbUpdateStudent.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/projection/dbUpdateTuitionPayment.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/projection/dbInsertCourse.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/projection/dbInsertEnrollment.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/projection/dbListStudents.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/projection/dbFindStudentById.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/projection/dbFindCourseStudents.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/api/studentsPost.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/api/coursesPost.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/api/studentPost.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/api/studentsPayPost.kt`
- Modify: `example/src/main/kotlin/dev/oblac/eddi/example/college/api/enrollsPost.kt`

**Interfaces:**
- Consumes: every domain type from Task 5
- Produces: `student.id` / `course.id` hold the domain UUID; `Student.last` is `UUID?`

- [ ] **Step 1: Update the migration**

In `example/src/main/resources/db/migration/college/V1__Create_project_tables.sql`, change the `id` and `last` columns. `id` loses its default (the domain supplies it), and `last` becomes a UUID with no foreign key, because update ids are not rows in `eddi.events`:

```sql
CREATE TABLE IF NOT EXISTS college.student
(
    id            UUID PRIMARY KEY,
    seq           BIGINT      NOT NULL REFERENCES eddi.events (seq),
    last          UUID        NULL,
    first_name    TEXT        NOT NULL,
    last_name     TEXT        NOT NULL,
    email         TEXT        NOT NULL UNIQUE,
    payed         BOOLEAN     NOT NULL DEFAULT false,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

and:

```sql
CREATE TABLE IF NOT EXISTS college.course
(
    id          UUID PRIMARY KEY,
    seq         BIGINT      NOT NULL REFERENCES eddi.events (seq),
    name        TEXT        NOT NULL,
    instructor  TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Leave `V2__Create_course_enrolled_table.sql` unchanged — its UUID foreign keys now point at real domain ids without any edit.

- [ ] **Step 2: Update the table definitions**

`StudentTable.kt` — `id` is no longer client-defaulted and `last` becomes a UUID:

```kotlin
object StudentTable : Table("college.student") {
    val id = uuid("id")
    val seq = ulong("seq").references(DbEvents.sequence)
    val last = uuid("last").nullable()
    val firstName = text("first_name")
    val lastName = text("last_name")
    val email = text("email").uniqueIndex()
    val payed = bool("payed").default(false)
    val registeredAt = timestamp("registered_at")

    override val primaryKey = PrimaryKey(id)
}
```

Remove the now-unused `import java.util.*` if the IDE flags it.

`CourseTable.kt` — same treatment for `id`:

```kotlin
object CourseTable : Table("college.course") {
    val id = uuid("id")
    val seq = ulong("seq").references(DbEvents.sequence)
    val name = text("name")
    val instructor = text("instructor")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
```

- [ ] **Step 3: Update the write-side projections**

`dbInsertStudent.kt` — insert the domain id:

```kotlin
fun dbInsertStudent(envelope: EventEnvelope<StudentRegistered>): UUID = transaction {
    val event = envelope.event

    StudentTable.insert {
        it[id] = event.studentId.id
        it[seq] = envelope.sequence.value
        it[firstName] = event.firstName
        it[lastName] = event.lastName
        it[email] = event.email
        it[registeredAt] = event.registeredAt
    } get StudentTable.id
}
```

`dbInsertCourse.kt`:

```kotlin
fun dbInsertCourse(envelope: EventEnvelope<CoursePublished>): UUID = transaction {
    val event = envelope.event

    CourseTable.insert {
        it[id] = event.courseId.id
        it[seq] = envelope.sequence.value
        it[name] = event.courseName
        it[instructor] = event.instructor
        it[createdAt] = event.publishAt
    } get CourseTable.id
}
```

`dbUpdateStudent.kt` — key on `id`, and store this update's id as `last` so the next update's derived chain link matches:

```kotlin
fun dbUpdateStudent(envelope: EventEnvelope<StudentUpdated>): Int = transaction {
    val event = envelope.event

    StudentTable.update({ StudentTable.id eq event.student.id }) {
        event.firstName?.let { firstName ->
            it[StudentTable.firstName] = firstName
        }
        event.lastName?.let { lastName ->
            it[StudentTable.lastName] = lastName
        }
        it[StudentTable.last] = event.updateId.id
    }
}
```

`dbUpdateTuitionPayment.kt`:

```kotlin
fun dbUpdateTuitionPayment(envelope: EventEnvelope<TuitionPaid>): Int = transaction {
    val event = envelope.event

    StudentTable.update({ StudentTable.id eq event.student.id }) {
        it[payed] = true
    }
}
```

`dbInsertEnrollment.kt` — both seq→UUID resolution queries disappear, because the event now carries the ids directly:

```kotlin
package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.example.college.StudentEnrolledInCourse
import dev.oblac.eddi.example.college.projection.db.CourseEnrolledTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun dbInsertEnrollment(envelope: EventEnvelope<StudentEnrolledInCourse>): UUID = transaction {
    val event = envelope.event

    CourseEnrolledTable.insert {
        it[studentId] = event.student.id
        it[courseId] = event.course.id
        it[enrolledAt] = event.enrolledAt
    } get CourseEnrolledTable.id
}
```

- [ ] **Step 4: Update the read-side projections**

In `dbListStudents.kt`, change the `Student` model's `last` type:

```kotlin
data class Student(
    val id: UUID,
    val seq: Seq,
    val last: UUID?,
    val firstName: String,
    val lastName: String,
    val email: String,
    val payed: Boolean,
    val registeredAt: Instant
)
```

and in the same file's row mapper replace `last = row[StudentTable.last]?.toSeq()` with:

```kotlin
                last = row[StudentTable.last],
```

Apply the identical row-mapper change in `dbFindStudentById.kt` and `dbFindCourseStudents.kt`. Leave the `seq = row[StudentTable.seq].toSeq()` lines alone — `seq` is still an event sequence.

- [ ] **Step 5: Update the create endpoints to mint ids**

`Main.launch` is an `AsyncCommandHandler`, so it returns a *job* UUID, not an entity id — which is what the existing `// todo names are wrong` comment refers to. Now that the caller mints the identity, these endpoints can return the real id.

`api/studentsPost.kt` — replace the body of the `post` block:

```kotlin
fun Routing.apiStudents() {
    post("/api/students") {
        val body = call.receiveText()

        val node = Json.fromJson(body, StudentRequest::class)

        val firstName = node.firstName
        val lastName = node.lastName
        val studentId = StudentId(UUID.randomUUID())

        Main.launch(
            RegisterStudent(
                studentId,
                firstName, lastName, "${firstName.lowercase()}.${lastName.lowercase()}@college.edu"
            )
        ).fold(
            ifLeft = {
                call.respondText(
                    "Error: error",
                    ContentType.Text.Plain,
                    HttpStatusCode.BadRequest
                )
            },
            ifRight = {
                call.respondText(
                    Json.toJson(StudentResponse(studentId.id)),
                    ContentType.Application.Json,
                    HttpStatusCode.Accepted
                )
            }
        )
    }
}
```

Add `import dev.oblac.eddi.example.college.StudentId` to the file's imports.

`api/coursesPost.kt` — same shape:

```kotlin
        val courseId = CoursePublishedTag(UUID.randomUUID())

        Main.launch(
            PublishCourse(
                courseId = courseId,
                courseName = name,
                instructor = instructor
            )
        ).fold(
            ifLeft = {
                call.respondText(
                    "Error: error",
                    ContentType.Text.Plain,
                    HttpStatusCode.BadRequest
                )
            },
            ifRight = {
                call.respondText(
                    Json.toJson(NewCourseResponse(courseId.id)),
                    ContentType.Application.Json,
                    HttpStatusCode.Accepted
                )
            }
        )
```

Add `import dev.oblac.eddi.example.college.CoursePublishedTag`. Note this also fixes an existing copy-paste bug: the success branch currently responds with `StudentResponse` from a *course* endpoint.

- [ ] **Step 6: Update the remaining endpoints to use `student.id`**

`api/studentPost.kt` — the projection lookup stays (it 404-checks), but the tag now comes from `id`, and a fresh `updateId` is minted per update:

```kotlin
        val node = Json.fromJson(body, StudentUpdateRequest::class)
        val firstName = node.firstName
        val lastName = node.lastName

        Main.launch(
            UpdateStudent(
                StudentUpdatedTag(UUID.randomUUID()),
                StudentId(student.id),
                firstName,
                lastName
            )
        ).fold(
```

Add `import dev.oblac.eddi.example.college.StudentUpdatedTag`, and delete the now-unused `val seq = student.seq` line.

`api/studentsPayPost.kt` — replace `val seq = student.seq` and the launch:

```kotlin
        Main.launch(
            PayTuition(
                StudentId(student.id)
            )
        ).fold(
```

`api/enrollsPost.kt` — replace the launch arguments:

```kotlin
        Main.launch(
            EnrollStudentInCourse(
                StudentId(student.id),
                CoursePublishedTag(course.id),
            )
        ).fold(
```

- [ ] **Step 7: Verify the whole project compiles and all tests pass**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL — the first fully green build since Task 2. Pre-existing warnings in `DbEventStore.kt` (unchecked casts) and `ui/style.kt` (deprecated `unaryPlus`) are expected.

If the compiler reports an unused `student.last` read in the UI, leave it — `Student.last` is deliberately retained, per the spec's Out of Scope section.

- [ ] **Step 8: Commit**

```bash
git add example
git commit -m "feat(example): key projections and api on uuid identity"
```

---

### Task 7: End-to-end verification against a real database

The jsonb tag queries (Task 4) have no unit coverage — this is where they get exercised. A stale Postgres volume will fail Flyway validation because `V1` changed in place, so the reset is mandatory.

**Files:** none modified

**Interfaces:**
- Consumes: the complete implementation from Tasks 2-6

- [ ] **Step 1: Reset the database**

```bash
just infra-reset
```

Expected: the `eddi_postgres_data` volume is removed and the container comes back up empty.

- [ ] **Step 2: Start the application**

```bash
just run
```

Expected: Flyway applies both schemas without checksum errors, then `🚀 Projections started` and the Ktor server binds port 8080.

- [ ] **Step 3: Register a student and confirm the returned id is the domain id**

```bash
curl -s -X POST localhost:8080/api/students \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace"}'
```

Expected: a JSON body containing a `uuid`. Then confirm the projection row uses that same id — this is the check that identity is now singular rather than two parallel identifiers:

```bash
docker compose exec -T postgres psql -U eddi_user -d eddi -c \
  "select id, seq, last from college.student;"
docker compose exec -T postgres psql -U eddi_user -d eddi -c \
  "select seq, name, tags from eddi.events order by seq;"
```

Expected: `college.student.id` equals the UUID returned by the API, and the `StudentRegistered` row's `tags` reads `[{"StudentRegistered": "<that same uuid>"}]` — string-valued, self-tagged.

- [ ] **Step 4: Exercise pay, enroll, and update**

Use the student id from Step 3 as `<SID>`:

```bash
curl -s -X POST localhost:8080/api/students/<SID>/pay
curl -s -X POST localhost:8080/api/courses \
  -H 'Content-Type: application/json' \
  -d '{"name":"Analytical Engines","instructor":"Babbage"}'
```

Then with the returned course id as `<CID>`:

```bash
curl -s -X POST localhost:8080/api/enrolls \
  -H 'Content-Type: application/json' \
  -d '{"student":"<SID>","course":"<CID>"}'
curl -s -X POST localhost:8080/api/students/<SID> \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Grace","lastName":"Hopper"}'
```

Expected: each returns HTTP 202. These exercise `findEventByTag` (pay, tuition check), `findEventByMultipleTags` (enrolment, update chain), and the self-tag lookups — the paths with no unit coverage.

- [ ] **Step 5: Confirm the update chain links by id**

Run the update twice more, then:

```bash
docker compose exec -T postgres psql -U eddi_user -d eddi -c \
  "select seq, data->>'updateId' as update_id, data->>'last' as last from eddi.events where name = 'StudentUpdated' order by seq;"
```

Expected: each row's `last` equals the previous row's `update_id`, and the first row's `last` is null. Also confirm `college.student.last` matches the most recent `update_id`.

- [ ] **Step 6: Check the UI renders**

Open `http://localhost:8080/` and visit the students list, a course view, and the student edit page.
Expected: pages render, and the edit form no longer carries a hidden `last` input.

- [ ] **Step 7: Stop the app and commit any fixes**

If Steps 3-6 surfaced defects, fix them, re-run `./gradlew build`, and commit. If everything passed, there is nothing to commit — record the verification in the task notes and move on.

---

### Task 8: Retire the reflection deserializer if the tests allow

The ~80-line reflection path in `Json.kt` exists because `Tag` wrapped `Seq` wrapped `ULong` — double value-class nesting that flattens to a primitive `long` and defeats Jackson's Kotlin module. A single value class wrapping a `UUID` is the case Jackson normally handles, so this code may now be dead weight. **This is a hypothesis to test, not a known fact** — if the tests go red, revert and keep the code.

**Files:**
- Modify (possibly reverted): `eddi-json/src/main/kotlin/dev/oblac/eddi/json/Json.kt:65-148`

**Interfaces:**
- Consumes: the round-trip tests from Task 3, the processor tests from Task 5

- [ ] **Step 1: Reduce `fromNode` to a plain Jackson call**

Replace the entire body of `fromNode` with:

```kotlin
    fun <T> fromNode(node: JsonNode, clazz: Class<T>): T =
        objectMapper.treeToValue(node, clazz)
```

Delete the KDoc paragraph describing the value-class workaround, and remove the imports that become unused (`kotlin.reflect.full.primaryConstructor`, `kotlin.reflect.jvm.jvmErasure`, `ObjectNode`, and the `jvmDefault` helper if it is now unreferenced).

- [ ] **Step 2: Run the JSON tests**

Run: `./gradlew :eddi-json:test`
Expected: PASS, if the hypothesis holds. If any of the four round-trip tests fail — particularly the nullable-tag or defaulted-parameter cases — **revert this task entirely**:

```bash
git checkout -- eddi-json/src/main/kotlin/dev/oblac/eddi/json/Json.kt
```

and stop here. The reflection path stays; that is a valid outcome, not a failure.

- [ ] **Step 3: Run the full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Re-verify against the real database**

Repeat Task 7 Steps 1-5. Round-trip unit tests use synthetic fixtures; this confirms real stored events still deserialize.

- [ ] **Step 5: Commit (only if every check above passed)**

```bash
git add eddi-json
git commit -m "refactor(json): drop reflection deserializer now that tags wrap UUID"
```

---

## Notes for the executor

- **Do not "fix" `dbFindLastEventByTag`.** It ignores its ref's value and matches any event of that name. This is pre-existing, has no callers, and is explicitly out of scope.
- **Do not remove `Student.last`** from the projection model even though no UI reads it. Out of scope per the spec.
- **Do not rename `Tag`'s type parameter.** It is literally named `Event`, shadowing the `Event` interface, so `Tag` is unconstrained. Tightening it to `Tag<out E : Event>` is a separate cleanup.
- The full `./gradlew build` is expected to fail from Task 2 through Task 5. Use the per-module commands given in each task. Task 6 Step 7 is the first green full build.
