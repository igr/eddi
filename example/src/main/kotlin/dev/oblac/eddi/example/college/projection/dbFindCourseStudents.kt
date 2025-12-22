package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.example.college.projection.db.CourseEnrolledTable
import dev.oblac.eddi.example.college.projection.db.StudentTable
import dev.oblac.eddi.toSeq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class CourseStudents(
    val enrolled: List<Student>,
    val notEnrolled: List<Student>
)

fun dbFindCourseStudents(courseId: UUID): CourseStudents = transaction {
    val enrolledStudentIds = CourseEnrolledTable
        .selectAll()
        .where { CourseEnrolledTable.courseId eq courseId }
        .map { it[CourseEnrolledTable.studentId] }
        .toSet()

    val allStudents = StudentTable
        .selectAll()
        .map { row ->
            Student(
                id = row[StudentTable.id],
                seq = row[StudentTable.seq].toSeq(),
                firstName = row[StudentTable.firstName],
                lastName = row[StudentTable.lastName],
                email = row[StudentTable.email],
                payed = row[StudentTable.payed],
                registeredAt = row[StudentTable.registeredAt]
            )
        }

    val (enrolled, notEnrolled) = allStudents.partition { it.id in enrolledStudentIds }

    CourseStudents(enrolled, notEnrolled)
}