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

/**
 * Value-derived id of an email address: the same email always yields the same [EmailId], so
 * "a student with this email exists" is an id lookup inside the consistency boundary.
 */
@JvmInline
value class EmailId(override val id: UUID) : Id {
    companion object {
        fun of(email: String) = EmailId(UUID.nameUUIDFromBytes("email:$email".toByteArray()))
    }
}

data class StudentRegistered(
    val studentId: StudentId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant = Instant.now()
) : Event {
    override fun ids() = listOf(studentId, EmailId.of(email))
}

sealed interface RegisterStudentError : CommandError {
    data object StudentAlreadyExist : RegisterStudentError {
        override fun toString(): String = "Student with this email already exists"
    }
}

fun ensureUniqueEmail(es: EventStoreRepo) = commandProcessor<RegisterStudent> {
    ensure(
        es.findEventById<StudentRegistered>(EmailId.of(it.email)) == null
    ) { RegisterStudentError.StudentAlreadyExist }
}


operator fun RegisterStudent.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureUniqueEmail(es)
        emit { StudentRegistered(StudentId(UUID.randomUUID()), firstName, lastName, email) }
    }
