package dev.oblac.eddi

interface EventStore {

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