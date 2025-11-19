package dev.oblac.eddi.db

import dev.oblac.eddi.*
import dev.oblac.eddi.db.tables.Events
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

fun <E : Event> dbStoreEvent(correlationId: ULong, event: E, refs: Array<Tag>): EventEnvelope<E> {
    val eventName = event.eventName()

    val sequence = transaction {
        Events.insert {
            it[Events.correlationId] = correlationId
            it[Events.name] = eventName.value
            it[Events.data] = event
            it[Events.tags] = refs.map { t -> Ref(t.name, t.seq) }.toTypedArray()
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