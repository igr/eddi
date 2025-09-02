package dev.oblac.eddi

interface EventStore {

    /**
     * IN API
     * Stores the given event and returns an [EventEnvelope] containing the event and its metadata.
     * The event is stored internally for later processing.
     */
    fun <E: Event> storeEvent(correlationId: Long, event: E): EventEnvelope<E>

}

interface EventStoreRepo {

    /**
     * Returns total events stored count.
     */
    fun totalEventsStored(): Long

    /**
     * Returns events from a specific index.
     */
    fun findLastEvents(fromIndex: Int): List<EventEnvelope<Event>>

    /**
     * Finds the last even of the specified type associated with the given tag.
     */
    fun findLastTaggedEvent(eventType: EventType, tag: Tag): EventEnvelope<Event>?

    /**
     * Finds the last event associated with the given tag, regardless of event type.
     */
    fun findLastTaggedEvent(tag: Tag): EventEnvelope<Event>?

}