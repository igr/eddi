package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import dev.oblac.eddi.*
import java.time.Instant

data class UpdateStudent(
    val student: StudentRegisteredTag,
    val last: StudentUpdatedTag,
    val firstName: String?,
    val lastName: String?
) : Command

@JvmInline
value class StudentUpdatedTag(override val seq: Seq) : Tag<StudentUpdated>

data class StudentUpdated(
    val student: StudentRegisteredTag,
    val last: StudentUpdatedTag,
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

    data object ConflictDetected : UpdateStudentError {
        override fun toString(): String = "Conflict detected: student was updated by another process"
    }
}

fun ensureStudentExists(es: EventStoreRepo) = commandProcessor<UpdateStudent> {
    ensureNotNull(
        es.findEvent<StudentRegistered>(
            it.student.seq,
            StudentRegisteredEvent.NAME,
        )
    ) { UpdateStudentError.StudentNotFound }
    it
}

fun ensureHasUpdateFields() = commandProcessor<UpdateStudent> {
    ensure(it.firstName != null || it.lastName != null)
    { UpdateStudentError.NothingToUpdate }
    it
}

fun ensureNoConflict(es: EventStoreRepo) = commandProcessor<UpdateStudent> {
    val latestUpdate = es.findEventByMultipleTags<StudentUpdated>(
        StudentUpdatedEvent.NAME,
        it.student   // find StudentUpdated event tagged with this student
    )
    if (latestUpdate != null) {
        ensure(latestUpdate.sequence == it.last.seq) {
            UpdateStudentError.ConflictDetected
        }
    } else {
        ensure(it.last.seq == Seq.ZERO) {
            UpdateStudentError.ConflictDetected
        }
    }
    it
}


operator fun UpdateStudent.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureStudentExists(es)
        +ensureHasUpdateFields()
        +ensureNoConflict(es) // optimistic locking check
        emit { StudentUpdated(student, last, firstName, lastName) }
    }