package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.Seq
import dev.oblac.eddi.example.college.projection.db.StudentTable
import dev.oblac.eddi.toSeq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

data class Student(
    val id: UUID,
    val seq: Seq,
    val firstName: String,
    val lastName: String,
    val email: String,
    val payed: Boolean,
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
                seq = row[StudentTable.seq].toSeq(),
                firstName = row[StudentTable.firstName],
                lastName = row[StudentTable.lastName],
                email = row[StudentTable.email],
                payed = row[StudentTable.payed],
                registeredAt = row[StudentTable.registeredAt]
            )
        }
}

