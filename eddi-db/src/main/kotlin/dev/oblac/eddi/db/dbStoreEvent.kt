package dev.oblac.eddi.db

import dev.oblac.eddi.*
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

fun <E : Event> dbStoreEvent(correlationId: ULong, event: E, eventName: EventName, refs: Array<Ref>): EventEnvelope<E> {
    val sequence = transaction {
        DbEvents.insert {
            it[DbEvents.correlationId] = correlationId
            it[DbEvents.name] = eventName.value
            it[DbEvents.data] = Json.toNode(event)
            it[DbEvents.tags] = refs
            it[DbEvents.createdAt] = Instant.now()
        } get DbEvents.sequence
    }

    return EventEnvelope(
        sequence = Seq(sequence),
        correlationId = correlationId,
        event = event,
        eventName = eventName,
    )
}