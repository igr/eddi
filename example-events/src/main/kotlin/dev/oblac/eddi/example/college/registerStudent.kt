package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import dev.oblac.eddi.*
import java.time.Instant
import java.util.UUID

data class RegisterStudent(
    val firstName: String,
    val lastName: String,
    val email: String
) : Command

@JvmInline
value class StudentId(override val id: UUID) : Id

data class StudentRegistered(
    val studentId: StudentId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant = Instant.now()
) : Event {
    override fun ids() = listOf(studentId)
}

sealed interface RegisterStudentError : CommandError {
    data object StudentAlreadyExist : RegisterStudentError {
        override fun toString(): String = "Student with this email already exists"
    }
}

fun ensureUniqueEmail(es: EventStoreRepo) = commandProcessor<RegisterStudent> {
    ensure(
        es.findEvents<StudentRegistered>(mapOf("email" to it.email)).isEmpty()
    ) { RegisterStudentError.StudentAlreadyExist }
}


operator fun RegisterStudent.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureUniqueEmail(es)
        emit { StudentRegistered(StudentId(UUID.randomUUID()), firstName, lastName, email) }
    }
