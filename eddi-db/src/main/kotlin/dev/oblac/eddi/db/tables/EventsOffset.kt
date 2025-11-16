package dev.oblac.eddi.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object EventsOffset : Table("events_offset") {
    val lastSequence = ulong("last_seq")
    val updatedAt = timestamp("ts")

    override val primaryKey = PrimaryKey(lastSequence)
}
