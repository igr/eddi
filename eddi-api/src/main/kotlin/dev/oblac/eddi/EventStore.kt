package dev.oblac.eddi

interface EventStore {

    /**
     * IN API
     * Starts the event store processing, which typically involves processing and publishing stored events.
     */
    fun start()

    /**
     * IN API
     * Stores the given events array and returns an array of [EventEnvelope] containing the events and their metadata.
     * The events are stored internally for later processing.
     */
    fun storeEvents(correlationId: Long, events: Array<Event>): Array<EventEnvelope<Event>>

}