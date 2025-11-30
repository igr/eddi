package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.example.college.RegisterStudent
import dev.oblac.eddi.example.college.StudentRegistered

object RegisterNewStudentError : CommandError {
    override fun toString(): String = "Student with this email already exists"
}

/**
 * Registers a new student.
 */
fun registerNewStudent(
    emailExists: (String) -> Boolean,
    command: RegisterStudent
): Either<CommandError, StudentRegistered> =
    uniqueStudentEmail(emailExists, command.email)
        .map {
            StudentRegistered(
                firstName = command.firstName,
                lastName = command.lastName,
                email = command.email
            )
        }


private fun uniqueStudentEmail(
    emailExists: (String) -> Boolean,
    email: String
): Either<RegisterNewStudentError, Unit> = either {
    ensure(!emailExists(email)) {
        println("Student with email $email already exists")
        RegisterNewStudentError
    }
}