package dev.oblac.eddi.memory

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * Memory-based event store implementation.
 *
 * This implementation:
 * - Stores events persistently in memory using thread-safe collections
 * - Delegates outbox pattern functionality to EventStoreOutbox
 * - Maintains event ordering and provides retrieval capabilities
 * - Memory efficient: events stored only once, no separate outbox queue
 */
class MemoryEventStore: EventStore {

    // Persistent storage for all events (ordered by insertion)
    internal val storedEvents = mutableListOf<EventEnvelope<Event>>()
    private val storageMutex = Mutex()

    // Metrics and tracking
    internal val totalEventsStored = AtomicLong(0)

    override fun <E: Event> storeEvent(correlationId: Long, event: E): EventEnvelope<E> {
        return runBlocking {        // todo is it blocking?
            // Store event persistently (outbox pattern - store first)
            val envelope = storageMutex.withLock {
                val globalSeq = totalEventsStored.incrementAndGet()
                val envelope = EventEnvelope(
                    globalSeq,
                    correlationId = correlationId,
                    event = event,
                    timestamp = Instant.now()
                )
                storedEvents.add(envelope as EventEnvelope<Event>)
                envelope
            }

            // Event is now available for outbox processing via index comparison
            println("Event stored for processing: ${event::class.simpleName}")

            envelope
        }
    }

}