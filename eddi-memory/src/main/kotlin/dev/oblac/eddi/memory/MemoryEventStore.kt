package dev.oblac.eddi.memory

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventBus
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
class MemoryEventStore(
    private val eventBus: EventBus,
    private val processingDelayMs: Long = 100L // Configurable delay for outbox processing
) : EventStore {
    
    // Persistent storage for all events (ordered by insertion)
    private val storedEvents = mutableListOf<EventEnvelope<Event>>()
    private val storageMutex = Mutex()
    
    // Outbox for handling asynchronous event publishing
    private lateinit var outbox: EventStoreOutbox
    
    // Metrics and tracking
    private val totalEventsStored = AtomicLong(0)

    override fun storeEvent(correlationId: Long, event: Event): EventEnvelope<Event> {
        return runBlocking {
            val envelope = EventEnvelope(
                id = correlationId,
                event = event,
                timestamp = Instant.now()
            )
            
            // Store event persistently (outbox pattern - store first)
            storageMutex.withLock {
                storedEvents.add(envelope)
                totalEventsStored.incrementAndGet()
            }
            
            // Event is now available for outbox processing via index comparison
            println("Event stored for processing: ${event::class.simpleName}")
            
            envelope
        }
    }

    override fun start() {
        println("Starting MemoryEventStore...")
        
        // Initialize and start the outbox
        outbox = EventStoreOutbox(eventBus, this, processingDelayMs)
        outbox.start()
        
        println("MemoryEventStore started with outbox processing")
    }
    
    /**
     * Stops the event processing.
     */
    fun stop() {
        println("Stopping MemoryEventStore...")
        if (::outbox.isInitialized) {
            outbox.stop()
        }
    }
    
    /**
     * Retrieves all stored events, optionally filtered by correlation ID.
     */
    suspend fun getStoredEvents(correlationId: Long? = null): List<EventEnvelope<Event>> {
        return storageMutex.withLock {
            if (correlationId != null) {
                storedEvents.filter { it.id == correlationId }
            } else {
                storedEvents.toList() // Return a copy to avoid concurrent modification
            }
        }
    }
    
    /**
     * Retrieves events stored after a specific timestamp.
     */
    suspend fun getEventsAfter(timestamp: Instant): List<EventEnvelope<Event>> {
        return storageMutex.withLock {
            storedEvents.filter { it.timestamp.isAfter(timestamp) }
        }
    }
    
    /**
     * Gets the total number of events stored and published.
     */
    fun getMetrics(): EventStoreMetrics {
        val storedCount = totalEventsStored.get()
        val publishedCount = if (::outbox.isInitialized) outbox.getTotalEventsPublished() else 0L
        val pendingCount = if (::outbox.isInitialized) outbox.getPendingEventsCount() else 0L
        return EventStoreMetrics(
            totalStored = storedCount,
            totalPublished = publishedCount,
            pendingInOutbox = pendingCount
        )
    }
    
    /**
     * Internal method for outbox to access storage mutex.
     * Package-private for use by EventStoreOutbox only.
     */
    internal fun getStorageMutex(): Mutex = storageMutex
    
    /**
     * Internal method for outbox to access stored events.
     * Package-private for use by EventStoreOutbox only.
     */
    internal fun getStoredEventsInternal(): List<EventEnvelope<Event>> = storedEvents
    
    /**
     * Internal method for outbox to get total events stored count.
     * Package-private for use by EventStoreOutbox only.
     */
    internal fun getTotalEventsStored(): Long = totalEventsStored.get()
}

/**
 * Metrics data class for monitoring event store performance.
 */
data class EventStoreMetrics(
    val totalStored: Long,
    val totalPublished: Long,
    val pendingInOutbox: Long
)