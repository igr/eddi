package dev.oblac.eddi.db.tables

import dev.oblac.eddi.json.Json
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb

object Events : Table("events") {
    val sequence = ulong("seq").autoIncrement()
    val correlationId = ulong("cid")
    val name = text("name")
    val data = jsonb("data",
        { Json.toEventJson(it) },
        { Json.fromEventJson(it) }
    )
    val createdAt = timestamp("ts")

    override val primaryKey = PrimaryKey(sequence)
}