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
) : Event

sealed interface PayTuitionError : CommandError {
    object StudentNotFound : PayTuitionError
    object TuitionAlreadyPaid : PayTuitionError
}

fun ensurePayTuitionStudentExists(es: EventStoreRepo) = commandProcessor<PayTuition> {
    ensureNotNull(
        es.findEventByTag<StudentRegistered>(
            StudentRegisteredEvent.NAME,
            it.student
        )
    ) { PayTuitionError.StudentNotFound }
}

fun ensureTuitionNotAlreadyPaid(es: EventStoreRepo) = commandProcessor<PayTuition> {
    ensure(
        es.findEventByTag(
            TuitionPaidEvent.NAME,
            it.student
        ) == null
    ) { PayTuitionError.TuitionAlreadyPaid }
}

operator fun PayTuition.invoke(es: EventStoreRepo) =
    process(this) {
        +ensurePayTuitionStudentExists(es)
        +ensureTuitionNotAlreadyPaid(es)
        emit { TuitionPaid(student) }
    }

/**
 * Meta companion class for [TuitionPaid].
 */
object TuitionPaidEvent : EventMeta<TuitionPaid> {

    override val CLASS = TuitionPaid::class
    override val NAME = EventName.of(CLASS)

    override fun refs(event: TuitionPaid): Array<Ref> = arrayOf(
        Ref(StudentRegisteredEvent.NAME, event.student.id)
    )
}
