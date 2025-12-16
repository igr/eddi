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

    override fun <T : Event> findEventByTag(eventName: EventName, tagToFind: Tag<T>): EventEnvelope<T>? {
        val targetRef = Events.refOf(tagToFind)
        return dbFindEventByTag(eventName, targetRef) as EventEnvelope<T>?
    }

    override fun <T: Event> findEvents(name: EventName, dataFilters: Map<String, String>): List<EventEnvelope<T>> {
        return dbFindEventsByName(name.value, dataFilters) as List<EventEnvelope<T>>
    }

    override fun <T: Event> findEvent(seq: Seq, name: EventName): EventEnvelope<T>? {
        return dbFindEventBySeqAndName(seq, name.value) as EventEnvelope<T>?
    }


}