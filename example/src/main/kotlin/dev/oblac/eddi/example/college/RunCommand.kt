package dev.oblac.eddi.example.college

import arrow.core.Either
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.UnknownCommandError
import dev.oblac.eddi.asyncCommandHandler
import dev.oblac.eddi.commandHandler
import dev.oblac.eddi.example.college.cmd.registerNewStudent
import kotlinx.coroutines.Dispatchers

fun asyncCommands(es: EventStore) =
    asyncCommandHandler(Dispatchers.Default, commandHandler(es))

private fun commandHandler(es: EventStore) = commandHandler { command ->
    when (command) {
        is RegisterStudent -> registerNewStudent(es, command)
        else -> {
            println("Unknown command: $command")
            Either.Left(UnknownCommandError(command))
        }
    }
}

