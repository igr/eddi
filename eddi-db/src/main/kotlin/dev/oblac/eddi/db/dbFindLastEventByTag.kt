package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventName
import dev.oblac.eddi.Tag
import dev.oblac.eddi.db.tables.Events
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun dbFindLastEventByTag(tag: Tag): EventEnvelope<Event>? = transaction {
    val jsonExtractId = CustomFunction<String?>(
        functionName = "jsonb_extract_path_text",
        columnType = TextColumnType(),
        Events.data,
        org.jetbrains.exposed.sql.stringLiteral("id")
    )

    Events
        .selectAll()
        .where { jsonExtractId eq tag.id }
        .orderBy(Events.sequence, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}

private fun ResultRow.toEventEnvelope(): EventEnvelope<Event> {
    return EventEnvelope(
        sequence = this[Events.sequence],
        correlationId = this[Events.correlationId],
        event = this[Events.data],
        eventName = EventName(this[Events.name]),
        timestamp = this[Events.createdAt],
    )
}
