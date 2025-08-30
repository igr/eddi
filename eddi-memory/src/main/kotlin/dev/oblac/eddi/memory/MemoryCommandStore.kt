package dev.oblac.eddi.memory

import dev.oblac.eddi.Command
import dev.oblac.eddi.CommandBus
import dev.oblac.eddi.CommandEnvelope
import dev.oblac.eddi.CommandStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant

class MemoryCommandStore(
    private val commandBus: CommandBus
) : CommandStore {
    private val commandChannel = Channel<CommandEnvelope<Command>>(Channel.UNLIMITED)
    private val commandFlow: Flow<CommandEnvelope<Command>> = commandChannel.receiveAsFlow()
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun <T : Command> storeCommand(command: T): CommandEnvelope<T> {
        return CommandEnvelope(
            id = System.currentTimeMillis(),
            command = command,
            timestamp = Instant.now(),
        ).also {
            val result = commandChannel.trySend(it as CommandEnvelope<Command>)
            if (result.isFailure) {
                println("Failed to store command: ${result.exceptionOrNull()}")
            }
            println("Storing command: $it")
        }
    }

    override fun publishCommand(commandEnvelope: CommandEnvelope<Command>) {
        commandBus.publishCommand(commandEnvelope)
    }

    override fun start() {
        scope.launch {
            commandFlow.collect {
                println("Processing command: $it")
                publishCommand(it)
            }
        }
    }

}
