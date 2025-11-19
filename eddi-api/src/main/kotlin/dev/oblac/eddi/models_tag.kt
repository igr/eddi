package dev.oblac.eddi

/**
 * Interface for event tags.
 */
interface Tag {
    val name: EventName
    val seq: ULong
}

/**
 * Reference to an event.
 */
data class Ref(
    override val name: EventName,
    override val seq: ULong
) : Tag
