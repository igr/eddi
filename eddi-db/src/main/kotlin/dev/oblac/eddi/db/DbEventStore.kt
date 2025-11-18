package dev.oblac.eddi.db

import dev.oblac.eddi.*

class DbEventStore : EventStoreInbox, EventStoreRepo {

    override fun <E : Event> storeEvent(correlationId: ULong, event: E): EventEnvelope<E> {
        return dbStoreEvent(correlationId, event)
    }

    private val eventProcessor = DbEventProcessor(processorId = 1L)

    fun startInbox(eventListener: EventListener) {
        eventProcessor.startInbox(eventListener)
    }

    override fun <E: Event> findLastEventByTagBefore(lastEvent: EventEnvelope<E>, ref: RefTag): EventEnvelope<Event>? {
        return dbFindLastEventByTag(lastEvent.sequence, ref)
    }

}