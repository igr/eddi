package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.example.college.CoursePublished
import dev.oblac.eddi.example.college.projection.db.CourseTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun dbInsertCourse(envelope: EventEnvelope<CoursePublished>): UUID = transaction {
    val event = envelope.event
    val seq = envelope.sequence

    CourseTable.insert {
        it[CourseTable.seq] = seq.value
        it[name] = event.courseName
        it[instructor] = event.instructor
        it[createdAt] = event.publishAt
    } get CourseTable.id
}