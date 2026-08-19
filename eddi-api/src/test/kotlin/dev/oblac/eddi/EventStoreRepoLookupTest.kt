package dev.oblac.eddi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

private data class Looked(val label: String) : Event {
    override fun ids() = emptyList<Id>()
}

@JvmInline
private value class LookedId(override val id: UUID) : Id

/** Records the event name each lookup was asked for. */
private class RecordingRepo : EventStoreRepo {
    var askedFor: EventName? = null

    override fun <T : Event> findEventById(eventName: EventName, id: Id): EventEnvelope<T>? {
        askedFor = eventName
        return null
    }

    override fun <T : Event> findEventByMultipleIds(eventName: EventName, vararg ids: Id): EventEnvelope<T>? {
        askedFor = eventName
        return null
    }

    override fun <T : Event> findEvents(name: EventName, dataFilters: Map<String, String>): List<EventEnvelope<T>> {
        askedFor = name
        return emptyList()
    }
}

class EventStoreRepoLookupTest {

    private val repo = RecordingRepo()
    private val id = LookedId(UUID.randomUUID())

    @Test
    fun `findEventById asks for the reified event's name`() {
        repo.findEventById<Looked>(id)

        assertEquals(EventName("Looked"), repo.askedFor)
    }

    @Test
    fun `findEventByMultipleIds asks for the reified event's name`() {
        repo.findEventByMultipleIds<Looked>(id, id)

        assertEquals(EventName("Looked"), repo.askedFor)
    }

    @Test
    fun `findEvents asks for the reified event's name`() {
        repo.findEvents<Looked>(mapOf("label" to "x"))

        assertEquals(EventName("Looked"), repo.askedFor)
    }
}
