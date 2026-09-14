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
