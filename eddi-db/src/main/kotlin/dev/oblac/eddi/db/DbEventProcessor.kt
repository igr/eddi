package dev.oblac.eddi.db

import dev.oblac.eddi.EventListener
import dev.oblac.eddi.Seq
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Delivers stored events, in `seq` order, to a listener and remembers the last delivered `seq`
 * per [processorId] in `eddi.events_offsets`.
 *
 * Each event is delivered in its own READ COMMITTED transaction that (1) takes the processor's
 * advisory lock — so when several instances share a [processorId] only one delivers at a time —
 * (2) reads the offset under that lock, (3) runs the listener and (4) advances the offset.
 * Anything the listener writes through Exposed joins that transaction, so database projections
 * are updated exactly once per event; side effects outside the database are at-least-once.
 * Listeners must not append events: appends refuse to run inside an outer transaction.
 *
 * A failing delivery is rolled back, logged and retried with exponential backoff, forever:
 * a poison event blocks this processor visibly rather than being skipped.
 *
 * Polling `seq > last_seq` cannot skip an event because appends serialize their insert + commit
 * behind the global append lock (see `dbStoreEvent.kt`), so `seq` order equals commit order:
 * once `seq = N` is visible, every `seq < N` already is.
 */
class DbEventProcessor(
    private val processorId: Long,
    private val pollInterval: Duration = 100.milliseconds,
    private val initialBackoff: Duration = 100.milliseconds,
    private val maxBackoff: Duration = 5.seconds,
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startInbox(eventListener: EventListener) {
        scope.launch {
            var backoff = initialBackoff
            var failures = 0
            while (isActive) {
                val outcome = try {
                    deliverNext(eventListener)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failures++
                    val what = if (e is DeliveryFailure) "event #${e.sequence.value}" else "polling"
                    println("⚠️ Processor $processorId: $what failed (attempt $failures): ${e.cause ?: e}; retrying in $backoff")
                    delay(backoff)
                    backoff = minOf(backoff * 2, maxBackoff)
                    continue
                }
                backoff = initialBackoff
                failures = 0
                if (outcome != Outcome.Delivered) delay(pollInterval)
            }
        }
    }

    /** Stops delivering; returns once the in-flight delivery, if any, has finished. */
    fun stop() {
        runBlocking { scope.coroutineContext.job.cancelAndJoin() }
    }

    private enum class Outcome { Delivered, Idle, NotLeader }

    private class DeliveryFailure(val sequence: Seq, cause: Throwable) : RuntimeException(cause)

    private fun deliverNext(eventListener: EventListener): Outcome =
        transaction(Connection.TRANSACTION_READ_COMMITTED) {
            if (!tryAdvisoryXactLock("eddi:processor:$processorId")) return@transaction Outcome.NotLeader
            val event = dbFetchNextUnpublishedEvent(processorId) ?: return@transaction Outcome.Idle
            println("Dispatching event #${event.sequence.value}: ${event.event}")
            try {
                eventListener(event)
            } catch (e: Exception) {
                throw DeliveryFailure(event.sequence, e)
            }
            dbMarkEventAsPublished(processorId, event.sequence)
            Outcome.Delivered
        }
}
