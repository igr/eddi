package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.Seq
import dev.oblac.eddi.example.college.RegisterStudent
import dev.oblac.eddi.example.college.StudentAlreadyRegistered
import dev.oblac.eddi.example.college.StudentRegistered
import dev.oblac.eddi.example.college.StudentRegisteredEvent

/**
 * Registers a new student.
 */
fun registerNewStudent(es: EventStore, command: RegisterStudent): Either<CommandError, Seq> {
    studentsWithSameEmail(es, command.email)
        .firstOrNull()
        ?.let {
            println("Student with this email already registered, do not register again.")
            return StudentAlreadyRegistered(command.email).left()
        }

    val ee = es.storeEvent(
        StudentRegistered(
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email
        )
    )

    return ee.sequence.right()
}

private fun studentsWithSameEmail(
    es: EventStore,
    email: String
): List<EventEnvelope<StudentRegistered>> =
    es.findEvents(StudentRegisteredEvent.NAME, mapOf("email" to email))
