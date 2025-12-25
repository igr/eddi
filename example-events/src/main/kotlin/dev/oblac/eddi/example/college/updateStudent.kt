package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import dev.oblac.eddi.*
import java.time.Instant

data class UpdateStudent(
    val student: StudentRegisteredTag,
    val firstName: String?,
    val lastName: String?
) : Command

data class StudentUpdated(
    val student: StudentRegisteredTag,
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

operator fun UpdateStudent.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureStudentExists(es)
        +ensureHasUpdateFields()
        emit { StudentUpdated(student, firstName, lastName) }
    }