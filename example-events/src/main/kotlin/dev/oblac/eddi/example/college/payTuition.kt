package dev.oblac.eddi.example.college

import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import dev.oblac.eddi.*
import java.time.Instant

data class PayTuition(
    val student: StudentRegisteredTag,
) : Command

data class TuitionPaid(
    val student: StudentRegisteredTag,
    val paidAt: Instant = Instant.now(),
) : Event

sealed interface PayTuitionError : CommandError {
    object StudentNotFound : PayTuitionError
    object TuitionAlreadyPaid : PayTuitionError
}

fun ensurePayTuitionStudentExists(es: EventStoreRepo) = CommandProcessor<PayTuition> {
    either {
        ensureNotNull(
            es.findEvent<StudentRegistered>(
                it.student.seq,
                StudentRegisteredEvent.NAME,
            )
        ) { PayTuitionError.StudentNotFound }
        it
    }
}

fun ensureTuitionNotAlreadyPaid(es: EventStoreRepo) = CommandProcessor<PayTuition> {
    either {
        ensure(
            es.findEventByTag(
                TuitionPaidEvent.NAME,
                StudentRegisteredTag(it.student.seq)
            ) == null
        ) { PayTuitionError.TuitionAlreadyPaid }
        it
    }
}

operator fun PayTuition.invoke(es: EventStoreRepo) =
    process(this) {
        +ensurePayTuitionStudentExists(es)
        +ensureTuitionNotAlreadyPaid(es)
        emit { TuitionPaid(student) }
    }
