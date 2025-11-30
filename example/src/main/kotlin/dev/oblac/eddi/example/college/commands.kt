package dev.oblac.eddi.example.college

import arrow.core.Either
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.UnknownCommandError
import dev.oblac.eddi.commandHandler
import dev.oblac.eddi.example.college.cmd.registerNewStudent

/**
 * Main command handler that routes commands to their respective handlers.
 */
fun commandHandler(es: EventStore) = commandHandler { command ->
    when (command) {
        is RegisterStudent -> registerNewStudent(es, command)
        else -> {
            println("Unknown command: $command")
            Either.Left(UnknownCommandError(command))
        }
    }
}

