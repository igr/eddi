package dev.oblac.eddi.memory

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventBus
import dev.oblac.eddi.Projector
import kotlin.reflect.KClass

class MemoryProjector(val eventBus: EventBus) : Projector {
    private val projections = mutableMapOf<Long, (Any) -> Unit>()

    override fun <E : Event, P> projectorForEvent(eventType: KClass<E>, handler: (E) -> P) {
        eventBus.registerEventHandler(eventType, { event ->
            val projection = handler(event.event as E)
            println("Event ${event.event} to projection $projection")
            triggerProjection(event.id, projection)
            arrayOf()
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


}