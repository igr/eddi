package dev.oblac.eddi

import kotlinx.coroutines.*

fun interface EventListener {
    operator fun invoke(envelope: EventEnvelope<Event>)
}

fun interface CommandHandler {
    operator fun invoke(command: Command)
}

fun interface AsyncCommandHandler {
    suspend operator fun invoke(command: Command)

    fun launch(command: Command, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        CoroutineScope(dispatcher).launch {
            this@AsyncCommandHandler(command)
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
) = AsyncCommandHandler { command ->
    withContext(dispatcher) {
        handler(command)
    }
}

