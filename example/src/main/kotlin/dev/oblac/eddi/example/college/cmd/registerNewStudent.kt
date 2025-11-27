package dev.oblac.eddi.example.college.cmd

import dev.oblac.eddi.EventStore
import dev.oblac.eddi.example.college.RegisterStudent
import dev.oblac.eddi.example.college.StudentRegistered

/**
 * Registers a new student.
 */
fun registerNewStudent(es: EventStore, command: RegisterStudent) {

    //es.findEvent(StudentRegistered::class)

    es.storeEvent(
        StudentRegistered(
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email
        )
    )
}
