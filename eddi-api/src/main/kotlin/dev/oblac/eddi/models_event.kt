package dev.oblac.eddi

import java.time.Instant
import kotlin.reflect.KClass

@JvmInline
value class EventName(val value: String) {
    companion object Companion {
        fun of(event: KClass<*>) = EventName(event.simpleName ?: error("Event class must have a simple name"))
    }
}

/**
 * Marker interface for Events.
 */
interface Event

/**
 * Stored event.
 */
data class EventEnvelope<E : Event>(
    val sequence: ULong,
    val correlationId: ULong,   // todo add CorrelationId value type
    val event: E,
    val eventName: EventName,
    val timestamp: Instant = Instant.now(),
)

