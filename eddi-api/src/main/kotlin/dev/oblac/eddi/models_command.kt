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
/**
 * The append was refused: the version of the [stale] ids changed between the command's read and
 * its append. The command should be re-run against the current state.
 */
data class ConcurrencyConflict(val stale: Set<Id>) : CommandError
