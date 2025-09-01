package dev.oblac.eddi

import java.time.Instant
import kotlin.reflect.KClass

/**
 * Marker interface for commands.
 */
interface Command

@JvmInline
value class EventType(val name: String) {
    companion object {
        fun of(klass: KClass<*>) = EventType(klass.simpleName ?: error("Event class must have a simple name"))
    }
}

/**
 * Marker interface for Events.
 */
interface Event {
    companion object {
        inline fun <reified T : Event> type() = EventType.of(T::class)
    }
}

interface Tag {
    val id: String
}

data class CommandEnvelope<T : Command>(
    val id: Long,
    val command: T,
    val timestamp: Instant
)

data class EventEnvelope<E : Event>(
    val id: Long,   // todo add CorrelationId value type
    val event: E,
    val eventType: EventType = EventType.of(event::class),
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