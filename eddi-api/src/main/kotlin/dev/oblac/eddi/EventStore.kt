package dev.oblac.eddi

import kotlin.reflect.KClass

interface EventStore : EventStoreRepo {

    /**
     * IN API
     * Starts the event store processing, which typically involves processing and publishing stored events.
     */
    fun start()

    /**
     * IN API
     * Stores the given event and returns an [EventEnvelope] containing the event and its metadata.
     * The event is stored internally for later processing.
     */
    fun storeEvent(correlationId: Long, event: Event): EventEnvelope<Event>

}

interface EventStoreRepo {

    /**
     * Returns total events stored count.
     */
    fun totalEventsStored(): Long

    /**
     * Returns events from a specific index.
     */
    fun findLast(fromIndex: Int): List<EventEnvelope<Event>>

    /**
     * Finds the last event of a specific class [klass] with the given identifier [id].
     * Returns the event if found, or null if no such event exists.
     */
    fun findLastTaggedEvent(
        eventType: EventType,
        tagklass: KClass<out Tag>,
        id: String): EventEnvelope<Event>?

}