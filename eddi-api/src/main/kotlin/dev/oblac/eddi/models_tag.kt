package dev.oblac.eddi

/**
 * Interface for event tags.
 */
interface Tag<out Event> {
    val seq: ULong
}

/**
 * Reference to an event.
 */
data class Ref(
    val name: EventName,
    val seq: ULong
)
