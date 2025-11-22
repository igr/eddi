package dev.oblac.eddi

/**
 * Event tags.
 */
interface Tag<out Event> {
    val seq: ULong
}

/**
 * Untyped reference to an event.
 */
data class Ref(
    val name: EventName,
    val seq: ULong
)
