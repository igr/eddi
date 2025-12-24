package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.example.college.StudentEnrolledInCourse
import dev.oblac.eddi.example.college.projection.db.CourseEnrolledTable
import dev.oblac.eddi.example.college.projection.db.CourseTable
import dev.oblac.eddi.example.college.projection.db.StudentTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun dbInsertEnrollment(envelope: EventEnvelope<StudentEnrolledInCourse>): UUID = transaction {
    val event = envelope.event

    val studentId = StudentTable
        .selectAll()
        .where { StudentTable.seq eq event.student.seq.value }
        .single()[StudentTable.id]

    val courseId = CourseTable
        .selectAll()
        .where { CourseTable.seq eq event.course.seq.value }
        .single()[CourseTable.id]

    CourseEnrolledTable.insert {
        it[CourseEnrolledTable.studentId] = studentId
        it[CourseEnrolledTable.courseId] = courseId
        it[enrolledAt] = event.enrolledAt
    } get CourseEnrolledTable.id
}
