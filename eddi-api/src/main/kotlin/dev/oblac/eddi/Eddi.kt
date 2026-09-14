package dev.oblac.eddi

import arrow.core.Either

/**
 * Generic event listener functional interface.
 * Implementations of this interface can handle events wrapped in [EventEnvelope]s.
 */
fun interface EventListener {
    operator fun invoke(envelope: EventEnvelope<Event>)
}

/**
 * Generic command handler functional interface.
 * Implementations of this interface can handle commands and return either a [CommandError] or a result of type [R].
 *
 * @param R the result type of the command handler
 */
fun interface CommandHandler<R> {
    operator fun invoke(command: Command): Either<CommandError, R>
}

fun interface CommandProcessor<C : Command> {
    operator fun invoke(command: C): Either<CommandError, C>
}

/// Helpers

/**
 * Creates a [CommandHandler] from a lambda.
 */
fun <R> commandHandler(handler: (Command) -> Either<CommandError, R>): CommandHandler<R> =
    CommandHandler { command -> handler(command) }

/**
 * Extension function to apply a retry effect to a CommandHandler.
 * Re-runs the command while it fails with [ConcurrencyConflict], at most [times] re-runs after the
 * first attempt. Every other result is returned as is.
 */
fun <R> CommandHandler<R>.retryOnConflict(times: Int = 3): CommandHandler<R> {
    val target = this
    return CommandHandler { command ->
        var result = target(command)
        var retry = 0
        while (retry < times && result.leftOrNull() is ConcurrencyConflict) {
            retry++
            println("🔁 Command ${command::class.simpleName} conflicted, retry $retry/$times")
            result = target(command)
        }
        result
    }
}
