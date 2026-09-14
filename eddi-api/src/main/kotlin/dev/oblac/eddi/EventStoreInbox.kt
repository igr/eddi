package dev.oblac.eddi

import arrow.core.Either

interface EventStoreInbox {

    /**
     * Stores the given event associated with the provided correlation ID.
     * Returns an EventEnvelope containing metadata about the stored event.
     */
    fun <E : Event> storeEvent(event: E, correlationId: ULong = 0u): EventEnvelope<E>

    /**
     * Stores [event] only if every id in [expected] still has exactly that version; otherwise
     * stores nothing and returns [ConcurrencyConflict] naming the ids whose version changed.
     * Must not be called inside an outer transaction.
     */
    fun <E : Event> storeEvent(
        event: E,
        expected: Versions,
        correlationId: ULong = 0u
    ): Either<ConcurrencyConflict, EventEnvelope<E>>
}
