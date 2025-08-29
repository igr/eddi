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
    val timestamp: Instant,
)