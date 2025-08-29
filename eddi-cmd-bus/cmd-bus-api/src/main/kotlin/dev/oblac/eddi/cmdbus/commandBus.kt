package dev.oblac.eddi.cmdbus

import dev.oblac.eddi.Command
import dev.oblac.eddi.CommandEnvelope

interface CommandBus {

    /**
     * Starts the command bus transport mechanism.
     */
    fun start()

    fun publishCommand(command: CommandEnvelope<Command>)

    fun registerCommandHandler(handler: (CommandEnvelope<Command>) -> Unit)

    /**
     * Internal implementation that handles the command received from the bus.
     */
    fun handleCommand(commandEnvelope: CommandEnvelope<Command>)

}