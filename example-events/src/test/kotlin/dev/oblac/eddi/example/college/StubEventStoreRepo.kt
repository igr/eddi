package dev.oblac.eddi.example.college

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventName
import dev.oblac.eddi.EventStoreRepo
import dev.oblac.eddi.Seq
import dev.oblac.eddi.Id
import java.time.Instant

/**
 * In-memory [EventStoreRepo] for processor tests. Lookups match the event name and the ids the
 * event carries.
 */
@Suppress("UNCHECKED_CAST")
class StubEventStoreRepo(
    private val events: List<EventEnvelope<out Event>> = emptyList()
) : EventStoreRepo {

    override fun <T : Event> findEventById(eventName: EventName, id: Id): EventEnvelope<T>? =
        events.lastOrNull { it.eventName == eventName && id in it.event.ids() } as EventEnvelope<T>?

    override fun <T : Event> findEventByMultipleIds(
        eventName: EventName,
        vararg ids: Id
    ): EventEnvelope<T>? =
        events.lastOrNull { it.eventName == eventName && it.event.ids().containsAll(ids.toList()) } as EventEnvelope<T>?

    companion object {
        fun <E : Event> envelope(event: E, seq: Long = 1L): EventEnvelope<E> =
            EventEnvelope(Seq.of(seq), 0u, event, EventName.of(event::class), Instant.EPOCH)
    }
}
