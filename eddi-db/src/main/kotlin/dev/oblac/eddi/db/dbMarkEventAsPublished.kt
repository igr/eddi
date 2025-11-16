package dev.oblac.eddi.db

import dev.oblac.eddi.db.tables.EventsOffset
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.Instant

fun dbMarkEventAsPublished(sequence: ULong) {
    transaction {
        EventsOffset.upsert {
            it[lastSequence] = sequence
            it[updatedAt] = Instant.now()
        }
    }
}