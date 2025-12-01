package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.example.college.projection.db.StudentTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

data class Student(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant
)

fun dbListStudents(): List<Student> = transaction {
    StudentTable
        .selectAll()
        .orderBy(StudentTable.firstName to org.jetbrains.exposed.sql.SortOrder.ASC)
        .orderBy(StudentTable.lastName to org.jetbrains.exposed.sql.SortOrder.ASC)
        .map { row ->
            Student(
                id = row[StudentTable.id],
                firstName = row[StudentTable.firstName],
                lastName = row[StudentTable.lastName],
                email = row[StudentTable.email],
                registeredAt = row[StudentTable.registeredAt]
            )
        }
}
