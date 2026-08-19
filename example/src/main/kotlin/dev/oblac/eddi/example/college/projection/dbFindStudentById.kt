package dev.oblac.eddi.example.college.projection

import dev.oblac.eddi.example.college.projection.db.StudentTable
import dev.oblac.eddi.toSeq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun dbFindStudentById(id: UUID): Student? = transaction {
    StudentTable
        .selectAll()
        .where { StudentTable.id eq id }
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
        .singleOrNull()
}