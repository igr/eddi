package dev.oblac.eddi

import arrow.core.Either
import kotlinx.coroutines.*

fun interface EventListener {
    operator fun invoke(envelope: EventEnvelope<Event>)
}

interface CommandHandler {
    operator fun <R> invoke(command: Command): Either<CommandError, R>
}

/**
 * Creates a [CommandHandler] from a lambda.
 */
fun commandHandler(handler: (Command) -> Either<CommandError, *>): CommandHandler = object : CommandHandler {
    override fun <R> invoke(command: Command): Either<CommandError, R> {
        @Suppress("UNCHECKED_CAST")
        return handler(command) as Either<CommandError, R>
    }
}

interface AsyncCommandHandler {
    suspend operator fun <R> invoke(command: Command): Either<CommandError, R>

    fun launch(command: Command, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        CoroutineScope(dispatcher).launch {
            this@AsyncCommandHandler<Any>(command)
        }
    }
}


/**
 * Wraps a synchronous [CommandHandler] to create an [AsyncCommandHandler]
 * that executes commands on the specified [dispatcher].
 */
fun asyncCommandHandler(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    handler: CommandHandler
): AsyncCommandHandler = object : AsyncCommandHandler {
    override suspend fun <R> invoke(command: Command): Either<CommandError, R> =
        withContext(dispatcher) {
            handler(command)
        }
}

