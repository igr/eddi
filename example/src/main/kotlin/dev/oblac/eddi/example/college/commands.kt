package dev.oblac.eddi.example.college

import arrow.core.Either
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.UnknownCommandError
import dev.oblac.eddi.commandHandler
import dev.oblac.eddi.example.college.cmd.invoke

/**
 * Main command handler that routes commands to their respective handlers.
 */
fun commandHandler(es: EventStore) = commandHandler { command ->
    when (command) {
        is RegisterStudent -> command(es)
        is UpdateStudent -> command(es)
        is PayTuition -> command(es)
        is PublishCourse -> command(es)
        is EnrollStudentInCourse -> command(es)
        else -> {
            println("Unknown command: $command")
            Either.Left(UnknownCommandError(command))
        }
    }.map {
        es.storeEvent(it)
    }
}

