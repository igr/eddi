package dev.oblac.eddi

/**
 * Interface for event tags.
 */
interface Tag {
    val seq: ULong
}

interface TTag<out Event> : Tag

/**
 * Reference to an event.
 */
data class Ref(
    val name: EventName,
    override val seq: ULong
) : Tag
