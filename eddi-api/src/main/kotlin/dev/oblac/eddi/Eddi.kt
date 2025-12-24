package dev.oblac.eddi

import arrow.core.Either
import arrow.core.right
import kotlinx.coroutines.*
import java.util.*

fun interface EventListener {
    operator fun invoke(envelope: EventEnvelope<Event>)
}

interface CommandHandler<R> {
    operator fun invoke(command: Command): Either<CommandError, R>
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
fun <R> commandHandler(handler: (Command) -> Either<CommandError, R>): CommandHandler<R> = object : CommandHandler<R> {
    override fun invoke(command: Command): Either<CommandError, R> {
        return handler(command)
    }
}

/**
 * Extension function to apply async execution effect to a CommandHandler.
 * Wraps the handler in an AsyncCommandHandler that executes commands asynchronously.
 */
fun <R> CommandHandler<R>.async(dispatcher: CoroutineDispatcher = Dispatchers.Default): AsyncCommandHandler<R> =
    AsyncCommandHandler(dispatcher, this)
