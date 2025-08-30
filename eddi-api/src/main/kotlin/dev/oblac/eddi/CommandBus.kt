package dev.oblac.eddi

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