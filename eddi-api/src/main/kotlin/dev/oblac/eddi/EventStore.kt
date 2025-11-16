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
    fun findLastTaggedEvent(eventName: EventName, tag: Tag): EventEnvelope<Event>?

    fun findLastTaggedEvent(eventName: EventName): EventEnvelope<Event>?

    /**
     * Finds the last event associated with the given tag, regardless of event type.
     */
    fun findLastTaggedEvent(tag: Tag): EventEnvelope<Event>?

}