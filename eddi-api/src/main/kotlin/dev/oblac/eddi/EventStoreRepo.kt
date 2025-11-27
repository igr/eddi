package dev.oblac.eddi

interface EventStoreRepo {

    /**
     * Finds the last event with the given [tagToFind] that occurred before the event with ID [lastEvent].
     */
    fun <T: Event> findLastEventByTagBefore(lastEvent: Seq, tagToFind: Tag<T>): EventEnvelope<T>?

    fun <T: Event> findEvents(name: EventName, dataFilters: Map<String, String> = mapOf()): List<EventEnvelope<T>>
}