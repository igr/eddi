package dev.oblac.eddi.sqlite

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.tags
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * SQLite-based event store implementation.
 */
class SqliteEventStore(
    private val eventStoreRepo: SqliteEventStoreRepo,
) : EventStore {

    private val storageMutex = Mutex()
    
    // Global sequence counter - in production, this should be managed by the database
    internal val totalEventsStored = AtomicLong(0)
    
    init {
        runBlocking {
            val existingCount = eventStoreRepo.totalEventsStored()
            totalEventsStored.set(existingCount)
        }

    }

    override fun <E : Event> storeEvent(correlationId: Long, event: E): EventEnvelope<E> {
        return runBlocking {
            // Get tags and their current sequence numbers for building history
            val tags = event.tags().associateWith { tag ->
                eventStoreRepo.findLastTaggedEvent(tag)?.sequence ?: 0L
            }

            // Store event persistently with proper locking
            val envelope = storageMutex.withLock {
                val globalSeq = totalEventsStored.incrementAndGet()
                val envelope = EventEnvelope(
                    sequence = globalSeq,
                    correlationId = correlationId,
                    event = event,
                    history = tags,
                    timestamp = Instant.now()
                )
                
                // Store in SQLite database
                eventStoreRepo.storeEventEnvelope(envelope as EventEnvelope<Event>)
                envelope
            }

            // Event is now available for outbox processing via sequence comparison
            //println("Event stored in SQLite: ${event::class.simpleName} (seq: ${envelope.sequence})")

            envelope
        }
    }
    
    /**
     * Gets current statistics about the event store.
     */
    fun stats(): DatabaseStats {
        return eventStoreRepo.calculateDatabaseStats()
    }
    
    /**
     * Closes the event store and underlying database connections.
     */
    fun close() {
        eventStoreRepo.close()
    }
}