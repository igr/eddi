package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import dev.oblac.eddi.*
import java.time.Instant
import java.util.UUID

data class UpdateStudent(
    val updateId: StudentUpdatedTag,
    val student: StudentId,
    val firstName: String?,
    val lastName: String?
) : Command

@JvmInline
value class StudentUpdatedTag(override val id: UUID) : Tag<StudentUpdated>

data class StudentUpdated(
    val updateId: StudentUpdatedTag,
    val student: StudentId,
    val last: StudentUpdatedTag?,   // there may be no previous update, so this is nullable
    val firstName: String?,
    val lastName: String?,
    val updatedAt: Instant = Instant.now()
) : Event

sealed interface UpdateStudentError : CommandError {
    data object NothingToUpdate : UpdateStudentError {
        override fun toString(): String = "No fields to update"
    }

    data object StudentNotFound : UpdateStudentError {
        override fun toString(): String = "Student not found"
    }
}

fun ensureStudentExists(es: EventStoreRepo) = commandProcessor<UpdateStudent> {
    ensureNotNull(
        es.findEventByTag<StudentRegistered>(
            StudentRegisteredEvent.NAME,
            it.student
        )
    ) { UpdateStudentError.StudentNotFound }
}

fun ensureHasUpdateFields() = commandProcessor<UpdateStudent> {
    ensure(it.firstName != null || it.lastName != null)
    { UpdateStudentError.NothingToUpdate }
}

operator fun UpdateStudent.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureStudentExists(es)
        +ensureHasUpdateFields()
        emit {
            // the previous update of this student, so updates form a chain
            val last = es.findEventByMultipleTags<StudentUpdated>(
                StudentUpdatedEvent.NAME,
                student
            )?.tag()
            StudentUpdated(updateId, student, last, firstName, lastName)
        }
    }

/**
 * Meta companion class for [StudentUpdated].
 */
object StudentUpdatedEvent : EventMeta<StudentUpdated> {

    override val CLASS = StudentUpdated::class
    override val NAME = EventName.of(CLASS)

    override fun refs(event: StudentUpdated): Array<Ref> = listOfNotNull(
        Ref(NAME, event.updateId.id),
        Ref(StudentRegisteredEvent.NAME, event.student.id),
        event.last?.let { Ref(NAME, it.id) }
    ).toTypedArray()
}

fun EventEnvelope<StudentUpdated>.tag() = event.updateId
