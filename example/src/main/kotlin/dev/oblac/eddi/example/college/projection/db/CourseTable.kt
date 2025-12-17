package dev.oblac.eddi.example.college.projection.db

import dev.oblac.eddi.db.tables.DbEvents
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

object CourseTable : Table("college.course") {
    val id = uuid("id").autoGenerate().clientDefault { UUID.randomUUID() }
    val seq = ulong("seq").references(DbEvents.sequence)
    val name = text("name")
    val instructor = text("instructor")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}