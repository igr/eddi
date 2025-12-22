package dev.oblac.eddi.example.college.projection.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

object CourseEnrolledTable : Table("college.course_enrolled") {
    val id = uuid("id").autoGenerate().clientDefault { UUID.randomUUID() }
    val studentId = uuid("student_id").references(StudentTable.id)
    val courseId = uuid("course_id").references(CourseTable.id)
    val enrolledAt = timestamp("enrolled_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(studentId, courseId)
    }
}