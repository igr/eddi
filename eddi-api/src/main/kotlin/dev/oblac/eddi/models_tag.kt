package dev.oblac.eddi

import java.util.UUID

/**
 * Event tags.
 */
interface Tag<out Event> {
    val id: UUID
}

/**
 * Untyped reference to an event.
 */
data class Ref(
    val name: EventName,
    val id: UUID
)
