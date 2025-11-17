package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventName
import dev.oblac.eddi.db.tables.Events
import dev.oblac.eddi.tags
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

fun <E : Event> dbStoreEvent(correlationId: ULong, event: E): EventEnvelope<E> {

    val eventName = EventName.of(event::class)
    val eventTags = event.tags()

    val sequence = transaction {
        Events.insert {
            it[Events.correlationId] = correlationId
            it[name] = eventName.value
            it[data] = event
            it[createdAt] = Instant.now()
        } get Events.sequence
    }

    return EventEnvelope(
        sequence = sequence,
        correlationId = correlationId,
        event = event,
        eventName = eventName,
    )
}