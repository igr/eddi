package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.db.tables.Events
import dev.oblac.eddi.eventName
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

fun <E : Event> dbStoreEvent(correlationId: ULong, event: E): EventEnvelope<E> {
    val eventName = event.eventName()

    val sequence = transaction {
        Events.insert {
            it[Events.correlationId] = correlationId
            it[Events.name] = eventName.value
            it[Events.data] = event
            it[Events.createdAt] = Instant.now()
        } get Events.sequence
    }

    return EventEnvelope(
        sequence = sequence,
        correlationId = correlationId,
        event = event,
        eventName = eventName,
    )
}