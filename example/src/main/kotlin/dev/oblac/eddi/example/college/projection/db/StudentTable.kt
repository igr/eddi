package dev.oblac.eddi.example.college.projection.db

import dev.oblac.eddi.db.tables.DbEvents
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

object StudentTable : Table("college.student") {
    val id = uuid("id").autoGenerate().clientDefault { UUID.randomUUID() }
    val seq = ulong("seq").references(DbEvents.sequence)
    val firstName = text("first_name")
    val lastName = text("last_name")
    val email = text("email").uniqueIndex()
    val registeredAt = timestamp("registered_at")

    override val primaryKey = PrimaryKey(id)
}