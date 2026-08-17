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
