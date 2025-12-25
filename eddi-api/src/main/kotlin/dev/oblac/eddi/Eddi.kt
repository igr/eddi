package dev.oblac.eddi

import arrow.core.Either
import arrow.core.right
import kotlinx.coroutines.*
import java.util.*

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


class AsyncCommandHandler<R>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val target: CommandHandler<R>
) : CommandHandler<UUID> {
    private val jobs = mutableMapOf<UUID, Job>()

    override fun invoke(command: Command): Either<CommandError, UUID> {
        val job = CoroutineScope(dispatcher).launch {
            target.invoke(command)
        }
        val jobId = UUID.randomUUID()
        jobs[jobId] = job
        job.invokeOnCompletion {
            // todo we should have timeout mechanism to clean up old jobs
            jobs.remove(jobId)
        }
        return jobId.right()
    }
}

/// Helpers

/**
 * Creates a [CommandHandler] from a lambda.
 */
fun <R> commandHandler(handler: (Command) -> Either<CommandError, R>): CommandHandler<R> =
    CommandHandler { command -> handler(command) }

/**
 * Extension function to apply async execution effect to a CommandHandler.
 * Wraps the handler in an AsyncCommandHandler that executes commands asynchronously.
 */
fun <R> CommandHandler<R>.async(dispatcher: CoroutineDispatcher = Dispatchers.Default): AsyncCommandHandler<R> =
    AsyncCommandHandler(dispatcher, this)
