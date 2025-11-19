package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.db.tables.DbEventsOffsets
import dev.oblac.eddi.db.tables.toEventEnvelope
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

data class UnpublishedEvents(
    val lastSeq: ULong,
    val latestSeq: ULong,
    val inSync: Boolean,
    val events: List<EventEnvelope<Event>>
)

fun dbFetchLastUnpublishedEvents(consumerId: Long, pageSize: Int = 1000): UnpublishedEvents = transaction {
    // 1. Read last published sequence
    val lastSeq = DbEventsOffsets
        .select(DbEventsOffsets.lastSequence)
        .where { DbEventsOffsets.id eq consumerId }
        .limit(1)
        .singleOrNull()?.get(DbEventsOffsets.lastSequence) ?: 0u

    // 2. Get latest event seq
    val latestSeq = DbEvents
        .select(DbEvents.sequence.max())
        .single()[DbEvents.sequence.max()] ?: lastSeq

    // 3. Fetch next batch of events (paginated)
    val events = DbEvents
        .selectAll()
        .where { DbEvents.sequence greater lastSeq }
        .orderBy(DbEvents.sequence, SortOrder.ASC)
        .limit(pageSize)
        .map { it.toEventEnvelope() }

    UnpublishedEvents(
        lastSeq = lastSeq,
        latestSeq = latestSeq,
        inSync = lastSeq == latestSeq,
        events = events
    )
}