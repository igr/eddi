package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.example.college.StudentUpdated
import dev.oblac.eddi.example.college.UpdateStudent

object UpdateExistingStudentError : CommandError {
    override fun toString(): String = "No fields to update"
}

/**
 * Updates an existing student.
 */
fun updateExistingStudent(
    command: UpdateStudent
): Either<CommandError, StudentUpdated> =
    validateUpdateFields(command)
        .map {
            StudentUpdated(
                student = command.student,
                firstName = command.firstName,
                lastName = command.lastName
            )
        }

private fun validateUpdateFields(
    command: UpdateStudent
): Either<UpdateExistingStudentError, Unit> = either {
    ensure(command.firstName != null || command.lastName != null) {
        println("No fields to update for student ${command.student}")
        UpdateExistingStudentError
    }
}