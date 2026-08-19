package dev.oblac.eddi

import java.util.UUID

/**
 * Event ids. An event is stored with the ids it carries and can be looked up by them.
 */
interface Id {
    val id: UUID
}
