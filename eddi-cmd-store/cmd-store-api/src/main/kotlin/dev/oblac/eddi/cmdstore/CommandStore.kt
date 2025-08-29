package dev.oblac.eddi.cmdstore

import dev.oblac.eddi.Command
import dev.oblac.eddi.CommandEnvelope
import dev.oblac.eddi.cmdbus.CommandBus

interface CommandStore {

    /**
     * IN API
     * Starts the command store processing, which typically involves processing and publishing stored commands.
     */
    fun start()

    /**
     * IN API
     * Stores the given command and returns a [CommandEnvelope] containing the command and its metadata.
     * The command is stored internally for later processing.
     */
    fun <T : Command> storeCommand(command: T): CommandEnvelope<T>

    /**
     * OUT API
     * Publishes the given [CommandEnvelope] to the outside world.
     * This function is typically called by the command store's internal processing mechanism.
     */
    fun publishCommand(commandEnvelope: CommandEnvelope<Command>)

}