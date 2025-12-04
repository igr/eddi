package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.example.college.PayTuition
import dev.oblac.eddi.example.college.StudentRegisteredTag
import dev.oblac.eddi.example.college.TuitionPaid

object PayStudentTuitionError : CommandError {
    override fun toString(): String = "Student not found"
}

/**
 * Pays student tuition.
 */
fun payStudentTuition(
    studentExists: (StudentRegisteredTag) -> Boolean,
    command: PayTuition
): Either<CommandError, TuitionPaid> =
    command.right()
        .flatMap { validateStudentExists(it, studentExists) }
        .map {
            TuitionPaid(
                student = it.student,
            )
        }

private fun validateStudentExists(
    command: PayTuition,
    studentExists: (StudentRegisteredTag) -> Boolean
): Either<PayStudentTuitionError, PayTuition> = either {
    ensure(studentExists(command.student)) {
        println("Student with seq ${command.student.seq} not found")
        PayStudentTuitionError
    }
    command
}
