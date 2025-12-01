package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.Seq
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.db.tables.toEventEnvelope
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun dbFindEventBySeqAndName(seq: Seq, name: String): EventEnvelope<Event>? = transaction {
    DbEvents
        .selectAll()
        .where { DbEvents.sequence eq seq.value }
        .andWhere { DbEvents.name eq name }
        .singleOrNull()
        ?.toEventEnvelope()
}
