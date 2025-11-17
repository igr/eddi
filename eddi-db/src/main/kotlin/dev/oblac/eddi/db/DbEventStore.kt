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

    override fun findLastEventByTag(tag: Tag): EventEnvelope<Event>? {
        return dbFindLastEventByTag(tag)
    }

}