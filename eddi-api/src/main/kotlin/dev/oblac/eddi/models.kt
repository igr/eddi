package dev.oblac.eddi

import java.time.Instant
import kotlin.reflect.KClass

/**
 * Marker interface for commands.
 */
interface Command

@JvmInline
value class EventName(val value: String) {
    companion object Companion {
        fun of(event: Event) = EventName(event::class.simpleName ?: error("Event class must have a simple name"))
        fun of(event: KClass<*>) = EventName(event.simpleName ?: error("Event class must have a simple name"))
    }
}

/**
 * Marker interface for Events.
 */
interface Event

fun Event.eventName(): EventName = EventName.of(this)

data class RefTag(
    val eventName: EventName,
    val sequence: ULong
)

/**
 * Interface for event references that can be converted to RefTag.
 */
interface Tag {
    val name: EventName
    val seq: ULong
    fun ref() = RefTag(name, seq)
}

data class EventEnvelope<E : Event>(
    val sequence: ULong,
    val correlationId: ULong,   // todo add CorrelationId value type
    val event: E,
    val eventName: EventName,
    val timestamp: Instant = Instant.now(),
)
