package dev.oblac.eddi.db

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import dev.oblac.eddi.*
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.json.Json
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.time.Instant

/**
 * Stores [event] unconditionally. Takes the per-id append lock on every id the event carries, so that
 * appends for any single id never interleave (see [lockIds]), then the global append lock (see [lockAppend]).
 * Must not be called inside an outer transaction.
 */
fun <E : Event> dbStoreEvent(correlationId: ULong, event: E, eventName: EventName, ids: List<Id>): EventEnvelope<E> {
    requireNoOuterTransaction()
    return transaction(Connection.TRANSACTION_READ_COMMITTED) {
        lockIds(ids)
        lockAppend()
        insertEvent(correlationId, event, eventName, ids)
    }
}

/**
 * Stores [event] only if every id in [expected] still has exactly that version; otherwise stores
 * nothing and returns [ConcurrencyConflict] naming the ids whose version changed.
 *
 * Runs in its own READ COMMITTED transaction: per-id locks first, then a fresh recheck of the
 * versions, then the global append lock and the insert. Must not be called inside an outer
 * transaction — an outer REPEATABLE READ snapshot would predate the locks and hide concurrent
 * commits from the recheck.
 */
fun <E : Event> dbStoreEventIf(
    correlationId: ULong,
    event: E,
    eventName: EventName,
    ids: List<Id>,
    expected: Versions
): Either<ConcurrencyConflict, EventEnvelope<E>> {
    requireNoOuterTransaction()
    return transaction(Connection.TRANSACTION_READ_COMMITTED) {
        lockIds(ids + expected.keys)
        val stale = expected.filter { (id, version) -> dbVersionOf(id) != version }.keys.toSet()
        if (stale.isNotEmpty()) {
            ConcurrencyConflict(stale).left()
        } else {
            lockAppend()
            insertEvent(correlationId, event, eventName, ids).right()
        }
    }
}

private const val APPEND_LOCK_KEY = "eddi:append"

/**
 * Every append is its own short transaction. An outer transaction would freeze the snapshot the
 * version recheck relies on, and would hold the global append lock until that transaction ends,
 * stalling every other append behind unrelated work.
 */
private fun requireNoOuterTransaction() {
    check(TransactionManager.currentOrNull() == null) {
        "Appends must not run inside an outer transaction: the version recheck needs a fresh snapshot, " +
            "and the global append lock would be held until that transaction ends"
    }
}

/**
 * Serializes appends per id: `pg_advisory_xact_lock` on each id, in sorted key order (deadlock-free),
 * held until the transaction ends. With every append holding the locks of the ids it carries,
 * `seq` order equals commit order for any single id, which is what makes `max(seq)` per id a
 * gap-free version.
 */
private fun Transaction.lockIds(ids: Collection<Id>) {
    ids.map { lockKey(it) }.toSortedSet().forEach { advisoryXactLock(it) }
}

/**
 * Serializes the insert + commit of every append behind one lock — always taken after the per-id
 * locks (no lock-order cycle) and held until the transaction ends. Result: for any two committed
 * events, `seq` order equals commit order, so a reader that has seen `seq = N` can rely on every
 * `seq < N` being visible. That is what makes `seq > last_seq` polling in [DbEventProcessor]
 * gap-free (numeric holes from rolled-back inserts are harmless). Cost: appends serialize on the
 * insert + commit (roughly 1–3 ms each); command reads and validation stay parallel.
 */
private fun Transaction.lockAppend() = advisoryXactLock(APPEND_LOCK_KEY)

private fun lockKey(id: Id): String = "${id::class.simpleName}:${id.id}"

private fun <E : Event> Transaction.insertEvent(
    correlationId: ULong,
    event: E,
    eventName: EventName,
    ids: List<Id>
): EventEnvelope<E> {
    val sequence = DbEvents.insert {
        it[DbEvents.correlationId] = correlationId
        it[DbEvents.name] = eventName.value
        it[DbEvents.data] = Json.valueToNode(event)
        it[DbEvents.ids] = Json.idsToNode(ids)
        it[DbEvents.createdAt] = Instant.now()
    } get DbEvents.sequence

    return EventEnvelope(
        sequence = Seq(sequence),
        correlationId = correlationId,
        event = event,
        eventName = eventName,
    )
}
