package dev.oblac.eddi

interface EventStoreRepo {

    /**
     * Finds the last event with the given [tagToFind] that occurred before the event with ID [lastEvent].
     */
    fun <T: Event> findLastEventByTagBefore(lastEvent: ULong, tagToFind: Tag<T>): EventEnvelope<T>?
}