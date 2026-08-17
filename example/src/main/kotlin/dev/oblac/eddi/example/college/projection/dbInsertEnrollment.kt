package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.example.college.StudentEnrolledInCourse
import dev.oblac.eddi.example.college.projection.db.CourseEnrolledTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun dbInsertEnrollment(envelope: EventEnvelope<StudentEnrolledInCourse>): UUID = transaction {
    val event = envelope.event

    CourseEnrolledTable.insert {
        it[studentId] = event.student.id
        it[courseId] = event.course.id
        it[enrolledAt] = event.enrolledAt
    } get CourseEnrolledTable.id
}
