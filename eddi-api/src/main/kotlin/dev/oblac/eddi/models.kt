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

data class CommandEnvelope<T : Command>(
    val id: Long,
    val command: T,
    val timestamp: Instant
)

data class EventEnvelope<E : Event>(
    val sequence: Long,
    val correlationId: Long,   // todo add CorrelationId value type
    val event: E,
    val eventType: EventType = EventType.of(event::class),
    val history: Map<Tag, Long>,
    val tags: Set<Tag> = event.tags(),
    val timestamp: Instant = Instant.now(),
)

data class Eddi(
    val commandBus: CommandBus,
    val commandStore: CommandStore,
    val eventBus: EventBus,
    val eventStore: EventStore,
    val eventStoreRepo: EventStoreRepo,
    val eventStoreOutbox: EventStoreOutbox,
    val serviceRegistry: ServiceRegistry,
    val projector: Projector
)