package dev.oblac.eddi.db

import dev.oblac.eddi.Id
import dev.oblac.eddi.Seq
import dev.oblac.eddi.db.tables.DbEvents
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Version of [id]: the sequence of the latest event carrying it, [Seq.ZERO] if none.
 * Inside an outer transaction this joins it (Exposed nests), which is what the conditional
 * append relies on to recheck versions under its locks.
 */
fun dbVersionOf(id: Id): Seq = transaction {
    val maxSeq = DbEvents.sequence.max()
    DbEvents
        .select(maxSeq)
        .where { idsContain(id) }
        .single()[maxSeq]
        ?.let { Seq(it) }
        ?: Seq.ZERO
}
