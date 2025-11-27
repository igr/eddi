package dev.oblac.eddi.example.college.cmd

import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.example.college.RegisterStudent
import dev.oblac.eddi.example.college.StudentRegistered
import dev.oblac.eddi.example.college.StudentRegisteredEvent

/**
 * Registers a new student.
 */
fun registerNewStudent(es: EventStore, command: RegisterStudent) {

    studentsWithSameEmail(es, command.email)
        .firstOrNull()
        ?.let {
            // Student with this email already registered, do not register again.
            return
        }

    es.storeEvent(
        StudentRegistered(
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email
        )
    )
}

private fun studentsWithSameEmail(
    es: EventStore,
    email: String
): List<EventEnvelope<StudentRegistered>> =
    es.findEvents(StudentRegisteredEvent.NAME, mapOf("email" to email))
