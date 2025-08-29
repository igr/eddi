package dev.oblac.eddi.eventbus

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope

interface EventBus {

    /**
     * Starts the event bus transport mechanism.
     */
    fun start()

    fun publishEvent(event: EventEnvelope<Event>)

    fun registerEventHandler(handler: (EventEnvelope<Event>) -> Unit)

    /**
     * Internal implementation that handles the event received from the bus.
     */
    fun handleEvent(eventEnvelope: EventEnvelope<Event>)

}