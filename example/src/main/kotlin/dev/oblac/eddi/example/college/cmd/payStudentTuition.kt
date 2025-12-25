package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.example.college.PayTuition
import dev.oblac.eddi.example.college.StudentRegistered
import dev.oblac.eddi.example.college.StudentRegisteredEvent
import dev.oblac.eddi.example.college.StudentRegisteredTag
import dev.oblac.eddi.example.college.TuitionPaid
import dev.oblac.eddi.example.college.TuitionPaidEvent
import dev.oblac.eddi.process

object PayTuitionStudentNotFoundError : CommandError {
    override fun toString(): String = "Student not found"
}

object TuitionAlreadyPaidError : CommandError {
    override fun toString(): String = "Tuition already paid"
}

fun ensurePayTuitionStudentExists(es: EventStore): (PayTuition) -> Either<PayTuitionStudentNotFoundError, PayTuition> =
    {
        either {
            ensureNotNull(
                es.findEvent<StudentRegistered>(
                    it.student.seq,
                    StudentRegisteredEvent.NAME,
                )
            ) { PayTuitionStudentNotFoundError }
            it
        }
    }

fun ensureTuitionNotAlreadyPaid(es: EventStore): (PayTuition) -> Either<TuitionAlreadyPaidError, PayTuition> =
    {
        either {
            ensure(
                es.findEventByTag(
                    TuitionPaidEvent.NAME,
                    StudentRegisteredTag(it.student.seq)
                ) == null
            ) { TuitionAlreadyPaidError }
            it
        }
    }

operator fun PayTuition.invoke(es: EventStore) =
    process(this) {
        +ensurePayTuitionStudentExists(es)
        +ensureTuitionNotAlreadyPaid(es)
        emit { TuitionPaid(student) }
    }
