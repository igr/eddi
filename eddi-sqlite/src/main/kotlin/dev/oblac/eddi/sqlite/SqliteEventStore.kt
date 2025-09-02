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
 *
 * This implementation:
 * - Stores events persistently in SQLite database using Exposed ORM
 * - Maintains thread safety using coroutines and mutexes
 * - Provides ACID guarantees through database transactions
 * - Optimized for high-throughput append-only workloads
 * - Supports the outbox pattern through ordered sequence numbers
 */
class SqliteEventStore(
    private val eventStoreRepo: SqliteEventStoreRepo,
    databasePath: String = "event_store.db"
) : EventStore {

    private val storageMutex = Mutex()
    
    // Global sequence counter - in production, this should be managed by the database
    internal val totalEventsStored = AtomicLong(0)
    
    init {
        // Initialize sequence counter from existing database
        runBlocking {
            val existingCount = eventStoreRepo.totalEventsStored()
            totalEventsStored.set(existingCount)
            eventStoreRepo.ensureInitialized()
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
            println("Event stored in SQLite: ${event::class.simpleName} (seq: ${envelope.sequence})")

            envelope
        }
    }
    
    /**
     * Batch store multiple events for improved performance.
     * Useful for bulk operations or replay scenarios.
     */
    suspend fun <E : Event> storeEvents(correlationId: Long, events: List<E>): List<EventEnvelope<E>> {
        if (events.isEmpty()) return emptyList()
        
        return storageMutex.withLock {
            val envelopes = events.map { event ->
                val tags = event.tags().associateWith { tag ->
                    eventStoreRepo.findLastTaggedEvent(tag)?.sequence ?: 0L
                }
                
                val globalSeq = totalEventsStored.incrementAndGet()
                EventEnvelope(
                    sequence = globalSeq,
                    correlationId = correlationId,
                    event = event,
                    history = tags,
                    timestamp = Instant.now()
                ) as EventEnvelope<Event>
            }
            
            // Batch insert for better performance
            eventStoreRepo.storeEventEnvelopes(envelopes)
            
            println("Batch stored ${events.size} events in SQLite")
            
            @Suppress("UNCHECKED_CAST")
            envelopes as List<EventEnvelope<E>>
        }
    }
    
    /**
     * Gets current statistics about the event store.
     */
    suspend fun getStats(): DatabaseStats {
        return eventStoreRepo.getDatabaseStats()
    }
    
    /**
     * Closes the event store and underlying database connections.
     */
    fun close() {
        eventStoreRepo.close()
    }
}