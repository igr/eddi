package dev.oblac.eddi.example.college

import arrow.core.Either
import dev.oblac.eddi.Event
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.UnknownCommandError
import dev.oblac.eddi.commandHandler

/**
 * Main command handler that routes commands to their respective handlers.
 * Each command decides against its own session and its event is appended through that session,
 * so the append fails with a conflict if anything the command read changed in the meantime.
 */
fun commandHandler(es: EventStore) = es.commandHandler<Event> { s, command ->
    when (command) {
        is RegisterStudent -> command(s)
        is UpdateStudent -> command(s)
        is PayTuition -> command(s)
        is PublishCourse -> command(s)
        is EnrollStudentInCourse -> command(s)
        else -> {
            println("Unknown command: $command")
            Either.Left(UnknownCommandError(command))
        }
    }
}
