package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.example.college.projection.db.CourseTable
import dev.oblac.eddi.toSeq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun dbFindCourseById(courseId: UUID): Course? = transaction {
    CourseTable
        .selectAll()
        .where { CourseTable.id eq courseId }
        .map { row ->
            Course(
                id = row[CourseTable.id],
                seq = row[CourseTable.seq].toSeq(),
                name = row[CourseTable.name],
                instructor = row[CourseTable.instructor],
                createdAt = row[CourseTable.createdAt]
            )
        }
        .singleOrNull()
}