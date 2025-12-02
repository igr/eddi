package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.example.college.StudentRegisteredTag
import dev.oblac.eddi.example.college.StudentUpdated
import dev.oblac.eddi.example.college.UpdateStudent

object UpdateExistingStudentError : CommandError {
    override fun toString(): String = "No fields to update"
}

object StudentNotFoundError : CommandError {
    override fun toString(): String = "Student not found"
}

/**
 * Updates an existing student.
 */
fun updateExistingStudent(
    studentExists: (StudentRegisteredTag) -> Boolean,
    command: UpdateStudent
): Either<CommandError, StudentUpdated> =
    command.right()
        .flatMap { validateStudentExists(it, studentExists) }
        .flatMap { validateUpdateFields(command) }
        .map {
            StudentUpdated(
                student = it.student,
                firstName = it.firstName,
                lastName = it.lastName
            )
        }

// TODO: this is important validation step!!!
private fun validateStudentExists(
    command: UpdateStudent,
    studentExists: (StudentRegisteredTag) -> Boolean
): Either<StudentNotFoundError, UpdateStudent> = either {
    val student = command.student
    ensure(studentExists(student)) {
        println("Student with seq ${student.seq} not found")
        StudentNotFoundError
    }
    command
}

private fun validateUpdateFields(
    command: UpdateStudent
): Either<UpdateExistingStudentError, UpdateStudent> = either {
    ensure(command.firstName != null || command.lastName != null) {
        println("No fields to update for student ${command.student}")
        UpdateExistingStudentError
    }
    command
}