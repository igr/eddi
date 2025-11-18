package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.RefTag
import dev.oblac.eddi.db.tables.Events
import dev.oblac.eddi.db.tables.toEventEnvelope
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun dbFindLastEventByTag(lastSequence: ULong, ref: RefTag): EventEnvelope<Event>? = transaction {
//    val jsonExtractId = CustomFunction<String?>(
//        functionName = "jsonb_extract_path_text",
//        columnType = TextColumnType(),
//        Events.data,
//        stringLiteral("id")
//    )

    Events
        .selectAll()
        .where { Events.sequence lessEq lastSequence }
        .andWhere { Events.name eq ref.eventName.value }
        .orderBy(Events.sequence, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}