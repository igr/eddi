package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.Tag
import dev.oblac.eddi.db.tables.Events
import dev.oblac.eddi.db.tables.toEventEnvelope
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun dbFindLastEventByTag(lastSequence: ULong, tag: Tag): EventEnvelope<Event>? = transaction {
    val jsonExtractId = CustomFunction<String?>(
        functionName = "jsonb_extract_path_text",
        columnType = TextColumnType(),
        Events.data,
        stringLiteral("id")
    )

    Events
        .selectAll()
        .where { Events.sequence lessEq lastSequence }
        .andWhere { jsonExtractId eq tag.id }
        .orderBy(Events.sequence, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}