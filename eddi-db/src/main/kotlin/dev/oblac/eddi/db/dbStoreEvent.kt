package dev.oblac.eddi.db

import dev.oblac.eddi.*
import dev.oblac.eddi.db.tables.DbEvents
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

fun <E : Event> dbStoreEvent(correlationId: ULong, event: E, eventName: EventName, refs: Array<Tag>): EventEnvelope<E> {
    val sequence = transaction {
        DbEvents.insert {
            it[DbEvents.correlationId] = correlationId
            it[DbEvents.name] = eventName.value
            it[DbEvents.data] = event
            it[DbEvents.tags] = refs.map { t -> Ref(t.name, t.seq) }.toTypedArray()
            it[DbEvents.createdAt] = Instant.now()
        } get DbEvents.sequence
    }

    return EventEnvelope(
        sequence = sequence,
        correlationId = correlationId,
        event = event,
        eventName = eventName,
    )
}