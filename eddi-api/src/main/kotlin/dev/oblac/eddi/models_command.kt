package dev.oblac.eddi

/**
 * Marker interface for commands.
 */
interface Command

/**
 * Marker interface for command errors.
 */
interface CommandError

data class UnknownCommandError(val command: Command) : CommandError {
    override fun toString(): String = "UnknownCommandError(command=$command)"
}