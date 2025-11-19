package dev.oblac.eddi.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object DbEventsOffsets : Table("events_offsets") {
    val id = long("id")
    val lastSequence = ulong("last_seq").references(DbEvents.sequence)
    val updatedAt = timestamp("ts")

    override val primaryKey = PrimaryKey(id)
}
