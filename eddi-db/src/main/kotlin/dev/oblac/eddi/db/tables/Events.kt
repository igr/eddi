package dev.oblac.eddi.db.tables

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventName
import dev.oblac.eddi.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb

object Events : Table("events") {
    val sequence = ulong("seq").autoIncrement()
    val correlationId = ulong("cid")
    val name = text("name")
    val data = jsonb("data",
        { Json.toJson(it) },
        { Json.fromJson<Event>(it) }
    )
    val createdAt = timestamp("ts")

    override val primaryKey = PrimaryKey(sequence)
}

// todo: implement generic insert function
//fun <E: Event> EventEnvelope<E>.foo(insert: UpdateBuilder<*>) {
//    insert[Events.correlationId] = this.correlationId
//    insert[Events.name] = this.eventName.value
//    insert[Events.data] = this.event
//    insert[Events.createdAt] = Instant.now()
//}

fun ResultRow.toEventEnvelope(): EventEnvelope<Event> {
    return EventEnvelope(
        sequence = this[Events.sequence],
        correlationId = this[Events.correlationId],
        event = this[Events.data],
        eventName = EventName(this[Events.name]),
        timestamp = this[Events.createdAt],
    )
}
