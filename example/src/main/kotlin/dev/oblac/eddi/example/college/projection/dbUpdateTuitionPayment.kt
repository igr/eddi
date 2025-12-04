package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.example.college.TuitionPaid
import dev.oblac.eddi.example.college.projection.db.StudentTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun dbUpdateTuitionPayment(envelope: EventEnvelope<TuitionPaid>): Int = transaction {
    val event = envelope.event

    StudentTable.update({ StudentTable.seq eq event.student.seq.value }) {
        it[payed] = true
    }
}
