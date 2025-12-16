package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventName
import dev.oblac.eddi.Ref
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.db.tables.toEventEnvelope
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun dbFindEventByTag(eventName: EventName, ref: Ref): EventEnvelope<Event>? = transaction {
    DbEvents
        .selectAll()
        .where { DbEvents.name eq eventName.value }
        .andWhere {
            val jsonExtract = CustomFunction<String?>(
                functionName = "jsonb_extract_path_text",
                columnType = TextColumnType(),
                DbEvents.tags,
                stringLiteral(ref.name.value)
            )
            jsonExtract eq ref.seq.value.toString()
        }
        .orderBy(DbEvents.sequence, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}