package dev.oblac.eddi

interface CommandStore {

    /**
     * Starts the command store processing, which typically involves processing and publishing stored commands.
     */
    fun start()

    /**
     * Stores the given command and returns a [CommandEnvelope] containing the command and its metadata.
     * The command is stored internally for later processing.
     */
    fun <T : Command> storeCommand(command: T): CommandEnvelope<T>

}