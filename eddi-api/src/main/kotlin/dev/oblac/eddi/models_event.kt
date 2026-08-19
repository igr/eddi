package dev.oblac.eddi

import java.time.Instant
import kotlin.reflect.KClass

/**
 * Sequence number.
 */
@JvmInline
value class Seq(val value: ULong) {
    fun toLong() = value.toLong()
    companion object Companion {
        val ZERO = Seq(0u)
        fun of(value: ULong) = Seq(value)
        fun of(value: Long) = Seq(value.toULong())
    }
}

fun ULong.toSeq() = Seq(this)
fun Long.toSeq() = Seq.of(this)
fun String.toSeq(): Seq = toLongOrNull()?.toSeq() ?: error("Invalid sequence number: $this")


@JvmInline
value class EventName(val value: String) {
    companion object Companion {
        fun of(event: KClass<*>) = EventName(event.simpleName ?: error("Event class must have a simple name"))
    }
}

/**
 * An event. Each event declares the [Id]s it carries: they are stored next to the event
 * and are what the id-based lookups on [EventStoreRepo] match against.
 */
interface Event {
    fun ids(): List<Id>
}

/**
 * Stored event.
 */
data class EventEnvelope<E : Event>(
    val sequence: Seq,
    val correlationId: ULong,   // todo add CorrelationId value type
    val event: E,
    val eventName: EventName,
    val timestamp: Instant = Instant.now(),
)

