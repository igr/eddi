package dev.oblac.eddi.db

import dev.oblac.eddi.db.tables.EventsOffsets
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.Instant

fun dbMarkEventAsPublished(processorId: Long, sequence: ULong) {
    transaction {
        EventsOffsets.upsert {
            it[id] = processorId
            it[lastSequence] = sequence
            it[updatedAt] = Instant.now()
        }
    }
}