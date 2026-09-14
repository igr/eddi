package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.db.tables.DbEventsOffsets
import dev.oblac.eddi.db.tables.toEventEnvelope
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * The first event after the processor's recorded offset, or null when the processor is in sync.
 * Inside an outer transaction this joins it, so [DbEventProcessor] reads the offset under its lock.
 */
fun dbFetchNextUnpublishedEvent(processorId: Long): EventEnvelope<Event>? = transaction {
    val lastSeq = DbEventsOffsets
        .select(DbEventsOffsets.lastSequence)
        .where { DbEventsOffsets.id eq processorId }
        .limit(1)
        .singleOrNull()?.get(DbEventsOffsets.lastSequence) ?: 0u

    DbEvents
        .selectAll()
        .where { DbEvents.sequence greater lastSeq }
        .orderBy(DbEvents.sequence, SortOrder.ASC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}
