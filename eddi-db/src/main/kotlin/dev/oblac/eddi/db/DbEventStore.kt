package dev.oblac.eddi.db

import dev.oblac.eddi.*

class DbEventStore : EventStoreInbox, EventStoreRepo {

    override fun <E : Event> storeEvent(correlationId: ULong, event: E): EventEnvelope<E> {
        val meta = Events.metaOf(event)
        return dbStoreEvent(correlationId, event, meta.NAME, meta.refs(event))
    }

    private val eventProcessor = DbEventProcessor(processorId = 1L)

    fun startInbox(eventListener: EventListener) {
        eventProcessor.startInbox(eventListener)
    }

    override fun <T: Event> findLastEventByTagBefore(lastEvent: Tag, tagToFind: TTag<T>): EventEnvelope<T>? {
        val targetRef = Events.refOf(tagToFind)
        return dbFindLastEventByTag(lastEvent.seq, targetRef) as EventEnvelope<T>?
    }

}