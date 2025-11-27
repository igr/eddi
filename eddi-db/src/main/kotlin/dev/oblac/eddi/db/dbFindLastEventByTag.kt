package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.Ref
import dev.oblac.eddi.Seq
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.db.tables.toEventEnvelope
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun dbFindLastEventByTag(lastSequence: Seq, ref: Ref): EventEnvelope<Event>? = transaction {
    DbEvents
        .selectAll()
        .where { DbEvents.sequence lessEq lastSequence.value }
        .andWhere { DbEvents.name eq ref.name.value }
        .orderBy(DbEvents.sequence, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}