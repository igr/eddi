package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.db.tables.Events
import dev.oblac.eddi.db.tables.EventsOffset
import org.jetbrains.exposed.sql.ResultRow
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

fun fetchLastUnpublishedEvents(pageSize: Int = 1000): UnpublishedEvents = transaction {
    // 1. Read last published sequence
    val lastSeq = EventsOffset
        .select(EventsOffset.lastSequence)
        .limit(1)
        .single()[EventsOffset.lastSequence]

    // 2. Get latest event seq
    val latestSeq = Events
        .select(Events.sequence.max())
        .single()[Events.sequence.max()] ?: lastSeq

    // 3. Fetch next batch of events (paginated)
    val events = Events
        .selectAll().where { Events.sequence greater lastSeq }
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

private fun ResultRow.toEventEnvelope(): EventEnvelope<Event> {
    return EventEnvelope(
        sequence = this[Events.sequence],
        correlationId = this[Events.correlationId],
        event = this[Events.data],
        eventName = dev.oblac.eddi.EventName(this[Events.name]),
        tags = this[Events.tags],
        timestamp = this[Events.createdAt],
    )
}