package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.Seq
import dev.oblac.eddi.example.college.projection.db.CourseTable
import dev.oblac.eddi.toSeq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

data class Course(
    val id: UUID,
    val seq: Seq,
    val name: String,
    val instructor: String,
    val createdAt: Instant
)

fun dbListCourses(): List<Course> = transaction {
    CourseTable
        .selectAll()
        .orderBy(CourseTable.name to SortOrder.ASC)
        .map { row ->
            Course(
                id = row[CourseTable.id],
                seq = row[CourseTable.seq].toSeq(),
                name = row[CourseTable.name],
                instructor = row[CourseTable.instructor],
                createdAt = row[CourseTable.createdAt]
            )
        }
}