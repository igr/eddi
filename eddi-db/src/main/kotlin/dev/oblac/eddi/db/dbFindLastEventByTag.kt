package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.Tag
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.db.tables.toEventEnvelope
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun dbFindLastEventByTag(lastSequence: ULong, tag: Tag): EventEnvelope<Event>? = transaction {
//    val jsonExtractId = CustomFunction<String?>(
//        functionName = "jsonb_extract_path_text",
//        columnType = TextColumnType(),
//        Events.data,
//        stringLiteral("id")
//    )

    DbEvents
        .selectAll()
        .where { DbEvents.sequence lessEq lastSequence }
        .andWhere { DbEvents.name eq tag.name.value }
        .orderBy(DbEvents.sequence, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}