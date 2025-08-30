package dev.oblac.eddi

import java.time.Instant

/**
 * Marker interface for commands.
 */
interface Command

/**
 * Marker interface for Events.
 */
interface Event

data class CommandEnvelope<T : Command>(
    val id: Long,
    val command: T,
    val timestamp: Instant
)


data class EventEnvelope<E : Event>(
    val id: Long,   // todo add CorrelationId value type
    val event: E,
    val timestamp: Instant = Instant.now(), // todo remove where set in the code
)

data class Eddi(
    val commandBus: CommandBus,
    val commandStore: CommandStore,
    val eventBus: EventBus,
    val evetStore: EventStore,
    val serviceRegistry: ServiceRegistry,
    val projector: Projector
)