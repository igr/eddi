package dev.oblac.eddi.projector

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.eventbus.EventBus
import kotlin.reflect.KClass

class ProjectorMemory : Projector {
    private val projections = mutableMapOf<Long, (Any) -> Unit>()
    private val eventHandlers = mutableMapOf<KClass<out Event>, (EventEnvelope<Event>) -> Unit>()

    override fun <E : Event, P> projectorForEvent(eventType: KClass<E>, handler: (E) -> P) {
        registerEventHandler(eventType, { event ->
            val projection = handler(event.event as E)
            println("Event ${event.event} to projection $projection")
            triggerProjection(event.id, projection)
        })
    }

    override fun <P> triggerProjection(correlationId: Long, projection: P) {
        projections[correlationId]?.invoke(projection as Any)
    }

    override fun <R> registerEphemeralProjectionWaitForCommandResult(cmdId: Long, resultHandler: (result: R) -> Unit) {
        projections[cmdId] = { projection ->
            resultHandler(projection as R)
            projections.remove(cmdId)
        }
    }

    override fun <E : Event> registerEventHandler(eventType: KClass<E>, handler: (EventEnvelope<E>) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        eventHandlers[eventType] = handler as (EventEnvelope<Event>) -> Unit
    }

    /**
     * Processes an incoming event by calling the appropriate registered handler.
     * This method would typically be called by an event bus or similar mechanism.
     */
    internal fun handleEvent(eventEnvelope: EventEnvelope<Event>) {
        val eventType = eventEnvelope.event::class
        eventHandlers[eventType]?.invoke(eventEnvelope)
    }

    override fun start(eventBus: EventBus) {
        eventBus.registerEventHandler(::handleEvent)
    }

}