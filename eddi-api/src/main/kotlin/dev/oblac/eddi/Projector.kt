package dev.oblac.eddi

import kotlin.reflect.KClass

interface Projector {

    /**
     * Registers a projector for a specific event type.
     * When an event of the specified type is received, the handler function will be called
     * to transform the event into a projection.
     */
    fun <E : Event, P> projectorForEvent(eventType: KClass<E>, handler: (E) -> P)

    /**
     * Triggers a projection for a given correlation ID.
     * This is typically called internally after an event has been processed into a projection.
     */
    fun <P> triggerProjection(correlationId: Long, projection: P)

    /**
     * Registers an ephemeral projection that waits for a command result.
     * The result handler will be called once when the projection is available,
     * and then the registration will be automatically removed.
     */
    fun <R> registerEphemeralProjectionWaitForCommandResult(cmdId: Long, resultHandler: (result: R) -> Unit)

}