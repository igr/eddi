package dev.oblac.eddi.db.tables

import dev.oblac.eddi.Event
import dev.oblac.eddi.Tag
import dev.oblac.eddi.db.Json
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb

object Events : Table("events") {
    val sequence = ulong("seq").autoIncrement()
    val correlationId = ulong("cid")
    val name = text("name")
    val data = jsonb("data", { Json.toJson(it)}, { Json.fromJson<Event>(it) })
    val tags = jsonb("tags", { Json.toJson(it)}, { Json.fromJson<Set<Tag>>(it) })
    val createdAt = timestamp("ts")

    override val primaryKey = PrimaryKey(sequence)
}