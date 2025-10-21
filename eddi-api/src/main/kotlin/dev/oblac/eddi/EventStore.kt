package dev.oblac.eddi


interface EventStoreRepo {

    /**
     * Returns total events stored count.
     */
    fun totalEventsStored(): Long

    /**
     * Returns events from a specific index.
     */
    fun findLastEvents(fromIndex: Int): Sequence<EventEnvelope<Event>>

    /**
     * Finds the last even of the specified type associated with the given tag.
     */
    fun findLastTaggedEvent(eventType: EventType, tag: Tag): EventEnvelope<Event>?

    fun findLastTaggedEvent(eventType: EventType): EventEnvelope<Event>?

    /**
     * Finds the last event associated with the given tag, regardless of event type.
     */
    fun findLastTaggedEvent(tag: Tag): EventEnvelope<Event>?

}