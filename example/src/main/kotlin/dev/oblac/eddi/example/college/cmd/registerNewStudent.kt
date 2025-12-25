package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.example.college.RegisterStudent
import dev.oblac.eddi.example.college.StudentRegistered
import dev.oblac.eddi.example.college.StudentRegisteredEvent
import dev.oblac.eddi.process

object RegisterNewStudentError : CommandError {
    override fun toString(): String = "Student with this email already exists"
}

fun ensureUniqueEmail(es: EventStore): (RegisterStudent) -> Either<RegisterNewStudentError, RegisterStudent> =
    {
        either {
            ensure(
                es.findEvents<StudentRegistered>(
                    StudentRegisteredEvent.NAME,
                    mapOf("email" to it.email)
                ).isEmpty()
            ) { RegisterNewStudentError }
            it
        }
    }


operator fun RegisterStudent.invoke(es: EventStore) =
    process(this) {
        +ensureUniqueEmail(es)
        emit { StudentRegistered(firstName, lastName, email) }
    }