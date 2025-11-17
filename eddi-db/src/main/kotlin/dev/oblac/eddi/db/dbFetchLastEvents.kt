package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.db.tables.Events
import dev.oblac.eddi.db.tables.EventsOffsets
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
    val lastSeq = EventsOffsets
        .select(EventsOffsets.lastSequence)
        .where { EventsOffsets.id eq consumerId }
        .limit(1)
        .singleOrNull()?.get(EventsOffsets.lastSequence) ?: 0u

    // 2. Get latest event seq
    val latestSeq = Events
        .select(Events.sequence.max())
        .single()[Events.sequence.max()] ?: lastSeq

    // 3. Fetch next batch of events (paginated)
    val events = Events
        .selectAll()
        .where { Events.sequence greater lastSeq }
        .orderBy(Events.sequence, SortOrder.ASC)
        .limit(pageSize)
        .map { it.toEventEnvelope() }

    UnpublishedEvents(
        lastSeq = lastSeq,
        latestSeq = latestSeq,
        inSync = lastSeq == latestSeq,
        events = events
    )
}