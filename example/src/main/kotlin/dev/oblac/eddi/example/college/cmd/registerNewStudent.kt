package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
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
    command.right()
        .flatMap { uniqueStudentEmail(it, emailExists) }
        .map {
            StudentRegistered(
                firstName = it.firstName,
                lastName = it.lastName,
                email = it.email
            )
        }


private fun uniqueStudentEmail(
    command: RegisterStudent,
    emailExists: (String) -> Boolean
): Either<RegisterNewStudentError, RegisterStudent> = either {
    ensure(!emailExists(command.email)) {
        println("Student with email ${command.email} already exists")
        RegisterNewStudentError
    }
    command
}