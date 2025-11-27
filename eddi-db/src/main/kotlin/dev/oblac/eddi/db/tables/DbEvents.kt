package dev.oblac.eddi.db.tables

import com.fasterxml.jackson.databind.JsonNode
import dev.oblac.eddi.*
import dev.oblac.eddi.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb

object DbEvents : Table("events") {
    val sequence = ulong("seq").autoIncrement()
    val correlationId = ulong("cid")
    val name = text("name")
    val data = jsonb("data",
        { Json.toJson(it) },
        { Json.jsonToNode(it) }
    )
    val tags = jsonb("tags",
        { Json.toJson(it) },
        { Json.fromJson<Array<Ref>>(it) }
    )
    val createdAt = timestamp("ts")

    override val primaryKey = PrimaryKey(sequence)
}

fun ResultRow.toEventEnvelope(): EventEnvelope<Event> {
    val eventName = EventName(this[DbEvents.name])
    val node: JsonNode = this[DbEvents.data]
    val klass = Events.metaOf(eventName).CLASS
    val event = Json.fromNode(node, klass.java)

    return EventEnvelope(
        sequence = Seq(this[DbEvents.sequence]),
        correlationId = this[DbEvents.correlationId],
        event = event,
        eventName = eventName,
        timestamp = this[DbEvents.createdAt],
    )
}
