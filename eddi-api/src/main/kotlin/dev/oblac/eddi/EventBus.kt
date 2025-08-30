package dev.oblac.eddi

import kotlin.reflect.KClass

interface EventBus {

    /**
     * Starts the event bus transport mechanism.
     */
    fun start()

    fun publishEvent(event: EventEnvelope<Event>)

    fun <E : Event> registerEventHandler(eventClass: KClass<E>, handler: (EventEnvelope<E>) -> Array<Event>)

    /**
     * Internal implementation that handles the event received from the bus.
     */
    fun handleEvent(eventEnvelope: EventEnvelope<Event>)

}

inline fun <reified E : Event> EventBus.registerEventHandler(noinline handler: (EventEnvelope<E>) -> Array<Event>) {
    registerEventHandler(E::class, handler)
}