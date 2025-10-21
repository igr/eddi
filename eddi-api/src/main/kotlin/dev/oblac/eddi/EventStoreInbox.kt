package dev.oblac.eddi

interface EventStoreInbox {

    /**
     * Stores the given event associated with the provided correlation ID.
     * Returns an EventEnvelope containing metadata about the stored event.
     */
    fun <E: Event> storeEvent(correlationId: Long, event: E): EventEnvelope<E>

}