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
    val sequence: Seq,
    val correlationId: ULong,   // todo add CorrelationId value type
    val event: E,
    val eventName: EventName,
    val timestamp: Instant = Instant.now(),
)

