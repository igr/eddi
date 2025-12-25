package dev.oblac.eddi.example.college

import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.oblac.eddi.*
import java.time.Instant

data class RegisterStudent(
    val firstName: String,
    val lastName: String,
    val email: String
) : Command

@JvmInline
value class StudentRegisteredTag(override val seq: Seq) : Tag<StudentRegistered>

data class StudentRegistered(
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant = Instant.now()
) : Event

sealed interface RegisterStudentError : CommandError {
    data object StudentAlreadyExist : RegisterStudentError {
        override fun toString(): String = "Student with this email already exists"
    }
}

fun ensureUniqueEmail(es: EventStoreRepo) = CommandProcessor<RegisterStudent> {
    either {
        ensure(
            es.findEvents<StudentRegistered>(
                StudentRegisteredEvent.NAME,
                mapOf("email" to it.email)
            ).isEmpty()
        ) { RegisterStudentError.StudentAlreadyExist }
        it
    }
}


operator fun RegisterStudent.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureUniqueEmail(es)
        emit { StudentRegistered(firstName, lastName, email) }
    }