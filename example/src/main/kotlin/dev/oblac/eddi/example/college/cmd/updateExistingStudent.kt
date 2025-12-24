package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.example.college.StudentRegistered
import dev.oblac.eddi.example.college.StudentRegisteredEvent
import dev.oblac.eddi.example.college.StudentUpdated
import dev.oblac.eddi.example.college.UpdateStudent
import dev.oblac.eddi.process

object UpdateExistingStudentError : CommandError {
    override fun toString(): String = "No fields to update"
}

object StudentNotFoundError : CommandError {
    override fun toString(): String = "Student not found"
}

fun ensureStudentExists(es: EventStore): (UpdateStudent) -> Either<StudentNotFoundError, UpdateStudent> =
    {
        either {
            ensureNotNull(
                es.findEvent<StudentRegistered>(
                    it.student.seq,
                    StudentRegisteredEvent.NAME,
                )
            ) { StudentNotFoundError }
            it
        }
    }

fun ensureHasUpdateFields(): (UpdateStudent) -> Either<UpdateExistingStudentError, UpdateStudent> =
    {
        either {
            ensure(it.firstName != null || it.lastName != null) { UpdateExistingStudentError }
            it
        }
    }

fun updateStudent(es: EventStore, command: UpdateStudent) =
    process(command) {
        +ensureStudentExists(es)
        +ensureHasUpdateFields()
        emit { StudentUpdated(student, firstName, lastName) }
    }