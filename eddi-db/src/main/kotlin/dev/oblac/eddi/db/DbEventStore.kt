package dev.oblac.eddi.db

import dev.oblac.eddi.*

class DbEventStore : EventStore {

    override fun <E : Event> storeEvent(event: E, correlationId: ULong): EventEnvelope<E> {
        val meta = Events.metaOf(event)
        return dbStoreEvent(correlationId, event, meta.NAME, meta.refs(event))
    }

    private val eventProcessor = DbEventProcessor(processorId = 1L)

    fun startInbox(eventListener: EventListener) {
        eventProcessor.startInbox(eventListener)
    }

    override fun <T: Event> findLastEventByTagBefore(lastEvent: Seq, tagToFind: Tag<T>): EventEnvelope<T>? {
        val targetRef = Events.refOf(tagToFind)
        return dbFindLastEventByTag(lastEvent, targetRef) as EventEnvelope<T>?
    }

}