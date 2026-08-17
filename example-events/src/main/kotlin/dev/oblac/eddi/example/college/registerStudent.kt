package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import dev.oblac.eddi.*
import java.time.Instant
import java.util.UUID

data class RegisterStudent(
    val studentId: StudentId,
    val firstName: String,
    val lastName: String,
    val email: String
) : Command

@JvmInline
value class StudentId(override val id: UUID) : Tag<StudentRegistered>

data class StudentRegistered(
    val studentId: StudentId,
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

fun ensureUniqueEmail(es: EventStoreRepo) = commandProcessor<RegisterStudent> {
    ensure(
        es.findEvents<StudentRegistered>(
            StudentRegisteredEvent.NAME,
            mapOf("email" to it.email)
        ).isEmpty()
    ) { RegisterStudentError.StudentAlreadyExist }
}


operator fun RegisterStudent.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureUniqueEmail(es)
        emit { StudentRegistered(studentId, firstName, lastName, email) }
    }

/**
 * Meta companion class for [StudentRegistered].
 */
object StudentRegisteredEvent : EventMeta<StudentRegistered> {

    override val CLASS = StudentRegistered::class
    override val NAME = EventName.of(CLASS)

    override fun refs(event: StudentRegistered): Array<Ref> = arrayOf(
        Ref(NAME, event.studentId.id)
    )
}

fun EventEnvelope<StudentRegistered>.tag() = event.studentId
