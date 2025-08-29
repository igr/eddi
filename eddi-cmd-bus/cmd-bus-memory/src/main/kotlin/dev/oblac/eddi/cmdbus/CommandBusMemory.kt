package dev.oblac.eddi.cmdbus

import dev.oblac.eddi.Command
import dev.oblac.eddi.CommandEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CommandBusMemory : CommandBus {
    private val commandChannel = Channel<CommandEnvelope<Command>>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.Default)
    private val handlers = mutableListOf<(CommandEnvelope<Command>) -> Unit>()

    override fun start() {
        scope.launch {
            commandChannel.receiveAsFlow().collect { commandEnvelope ->
                handleCommand(commandEnvelope)
            }
        }
    }

    override fun registerCommandHandler(handler: (CommandEnvelope<Command>) -> Unit) {
        handlers.add(handler)
    }

    override fun publishCommand(command: CommandEnvelope<Command>) {
        commandChannel.trySend(command)
        println("Published command: $command")
    }

    override fun handleCommand(commandEnvelope: CommandEnvelope<Command>) {
        println("Handling command: $commandEnvelope")
        handlers.forEach { handler ->
            handler(commandEnvelope)
        }
    }
}