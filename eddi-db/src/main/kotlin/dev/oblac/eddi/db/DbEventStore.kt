package dev.oblac.eddi.db

import dev.oblac.eddi.*

class DbEventStore : EventStore {

    override fun <E : Event> storeEvent(event: E, correlationId: ULong): EventEnvelope<E> =
        dbStoreEvent(correlationId, event, Events.nameOf(event), event.ids())

    private val eventProcessor = DbEventProcessor(processorId = 1L)

    fun startInbox(eventListener: EventListener) {
        eventProcessor.startInbox(eventListener)
    }

    override fun <T : Event> findEventById(eventName: EventName, id: Id): EventEnvelope<T>? =
        dbFindEventById(eventName, id) as EventEnvelope<T>?

    override fun <T : Event> findEventByMultipleIds(eventName: EventName, vararg ids: Id): EventEnvelope<T>? =
        dbFindEventByMultipleIds(eventName, *ids) as EventEnvelope<T>?

    override fun <T: Event> findEvents(name: EventName, dataFilters: Map<String, String>): List<EventEnvelope<T>> {
        return dbFindEventsByName(name.value, dataFilters) as List<EventEnvelope<T>>
    }



}