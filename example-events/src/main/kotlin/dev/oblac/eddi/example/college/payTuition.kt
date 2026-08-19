package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import dev.oblac.eddi.*
import java.time.Instant

data class PayTuition(
    val student: StudentId,
) : Command

data class TuitionPaid(
    val student: StudentId,
    val paidAt: Instant = Instant.now(),
) : Event {
    override fun ids() = listOf(student)
}

sealed interface PayTuitionError : CommandError {
    object StudentNotFound : PayTuitionError {
        override fun toString(): String = "Student not found"
    }

    object TuitionAlreadyPaid : PayTuitionError {
        override fun toString(): String = "Tuition has already been paid"
    }
}

fun ensurePayTuitionStudentExists(es: EventStoreRepo) = commandProcessor<PayTuition> {
    ensureNotNull(
        es.findEventById<StudentRegistered>(it.student)
    ) { PayTuitionError.StudentNotFound }
}

fun ensureTuitionNotAlreadyPaid(es: EventStoreRepo) = commandProcessor<PayTuition> {
    ensure(
        es.findEventById<TuitionPaid>(it.student) == null
    ) { PayTuitionError.TuitionAlreadyPaid }
}

operator fun PayTuition.invoke(es: EventStoreRepo) =
    process(this) {
        +ensurePayTuitionStudentExists(es)
        +ensureTuitionNotAlreadyPaid(es)
        emit { TuitionPaid(student) }
    }
