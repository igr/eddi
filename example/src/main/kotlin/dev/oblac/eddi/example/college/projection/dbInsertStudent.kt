package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.example.college.StudentRegistered
import dev.oblac.eddi.example.college.projection.db.StudentTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun dbInsertStudent(envelope: EventEnvelope<StudentRegistered>): UUID = transaction {
    val event = envelope.event
    val seq = envelope.sequence

    StudentTable.insert {
        it[StudentTable.seq] = seq.value
        it[firstName] = event.firstName
        it[lastName] = event.lastName
        it[email] = event.email
        it[registeredAt] = event.registeredAt
    } get StudentTable.id
}
