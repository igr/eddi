package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventName
import dev.oblac.eddi.Id
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.db.tables.toEventEnvelope
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun dbFindEventById(eventName: EventName, id: Id): EventEnvelope<Event>? = transaction {
    addLogger(StdOutSqlLogger)
    DbEvents
        .selectAll()
        .where { DbEvents.name eq eventName.value }
        .andWhere { idsContain(id) }
        .orderBy(DbEvents.sequence, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}


fun dbFindEventByMultipleIds(eventName: EventName, vararg ids: Id): EventEnvelope<Event>? = transaction {
    addLogger(StdOutSqlLogger)
    DbEvents
        .selectAll()
        .where { DbEvents.name eq eventName.value }
        .apply {
            ids.forEach { id -> andWhere { idsContain(id) } }
        }
        .orderBy(DbEvents.sequence, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}
