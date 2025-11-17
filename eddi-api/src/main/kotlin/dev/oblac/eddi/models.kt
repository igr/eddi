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
    }
}

/**
 * Marker interface for Events.
 */
interface Event

/**
 * Reflectively extracts all Tag properties from the Event instance.
 * Returns a set of Tag instances found in the Event.
 */
fun Event.tags(): Set<Tag> = this::class.members
    .filter { prop ->
        val kclass = prop.returnType.classifier as? KClass<*>
        kclass != null && Tag::class.java.isAssignableFrom(kclass.java)
    }
    .mapNotNull { runCatching { it.call(this) as? Tag }.getOrNull() }
    .toSet()

interface Tag {
    val id: String
}

data class EventEnvelope<E : Event>(
    val sequence: ULong,
    val correlationId: ULong,   // todo add CorrelationId value type
    val event: E,
    val eventName: EventName,
    val timestamp: Instant = Instant.now(),
)
