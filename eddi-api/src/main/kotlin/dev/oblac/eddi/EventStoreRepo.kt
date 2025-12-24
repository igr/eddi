package dev.oblac.eddi

interface EventStoreRepo {

    /**
     * Finds the last event with the given [tagToFind] that occurred before the event with ID [lastEvent].
     */
    fun <T: Event> findLastEventByTagBefore(lastEvent: Seq, tagToFind: Tag<T>): EventEnvelope<T>?


    fun <T: Event> findEventByTag(eventName: EventName, tagToFind: Tag<T>): EventEnvelope<T>?

    fun <T: Event> findEventByMultipleTags(eventName: EventName, vararg tagsToFind: Tag<Event>): EventEnvelope<T>?


    fun <T: Event> findEvents(name: EventName, dataFilters: Map<String, String> = mapOf()): List<EventEnvelope<T>>

    /**
     * Finds a specific event by its sequence ID and name.
     */
    fun <T: Event> findEvent(seq: Seq, name: EventName): EventEnvelope<T>?
}