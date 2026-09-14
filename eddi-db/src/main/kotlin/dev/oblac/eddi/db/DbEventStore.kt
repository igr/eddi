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
}
