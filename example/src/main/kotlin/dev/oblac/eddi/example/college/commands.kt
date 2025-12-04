package dev.oblac.eddi.example.college

import arrow.core.Either
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.UnknownCommandError
import dev.oblac.eddi.commandHandler
import dev.oblac.eddi.example.college.cmd.payStudentTuition
import dev.oblac.eddi.example.college.cmd.registerNewStudent
import dev.oblac.eddi.example.college.cmd.updateExistingStudent

/**
 * Main command handler that routes commands to their respective handlers.
 */
fun commandHandler(es: EventStore) = commandHandler { command ->
    when (command) {
        is RegisterStudent -> registerNewStudent(
            emailExists = { email ->
                es.findEvents<StudentRegistered>(
                    StudentRegisteredEvent.NAME,
                    mapOf("email" to email)
                ).isNotEmpty()
            }, command
        ).map {
            es.storeEvent(it)
        }

        is UpdateStudent -> updateExistingStudent(
            studentExists = { student ->
                es.findEvent<StudentRegistered>(
                    student.seq,
                    StudentRegisteredEvent.NAME,
                ) != null
            },
            command
        ).map {
            es.storeEvent(it)
        }

        is PayTuition -> payStudentTuition(
            studentExists = { student ->
                es.findEvent<StudentRegistered>(
                    student.seq,
                    StudentRegisteredEvent.NAME,
                ) != null
            },
            command
        ).map {
            es.storeEvent(it)
        }

        else -> {
            println("Unknown command: $command")
            Either.Left(UnknownCommandError(command))
        }
    }
}

