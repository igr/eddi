package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.example.college.StudentUpdated
import dev.oblac.eddi.example.college.projection.db.StudentTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun dbUpdateStudent(envelope: EventEnvelope<StudentUpdated>): Int = transaction {
    val event = envelope.event

    StudentTable.update({ StudentTable.seq eq event.student.seq.value }) {
        event.firstName?.let { firstName ->
            it[StudentTable.firstName] = firstName
        }
        event.lastName?.let { lastName ->
            it[StudentTable.lastName] = lastName
        }
    }
}