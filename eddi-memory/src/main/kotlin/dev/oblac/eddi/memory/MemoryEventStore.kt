package dev.oblac.eddi.memory

import dev.oblac.eddi.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

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
     * Retrieves events stored after a specific timestamp.
     */
    fun findEventsAfter(timestamp: Instant): List<EventEnvelope<Event>> {
        return storedEvents.filter { it.timestamp.isAfter(timestamp) }
    }
    
    /**
     * Gets the total number of events stored and published.
     */
    fun metrics(): EventStoreMetrics {
        val storedCount = totalEventsStored.get()
        val publishedCount = if (::outbox.isInitialized) outbox.getTotalEventsPublished() else 0L
        val pendingCount = if (::outbox.isInitialized) outbox.getPendingEventsCount() else 0L
        return EventStoreMetrics(
            totalStored = storedCount,
            totalPublished = publishedCount,
            pendingInOutbox = pendingCount
        )
    }

    override fun totalEventsStored(): Long = totalEventsStored.get()

    override fun findLast(fromIndex: Int): List<EventEnvelope<Event>> = storedEvents.subList(fromIndex, storedEvents.size)

    override fun findLastTaggedEvent(eventType: EventType, tagklass: KClass<out Tag>, id: String): EventEnvelope<Event>? {
        return storedEvents.lastOrNull {
            if (it.eventType != eventType) {
                return@lastOrNull false
            }
            val event = it.event
            val typedId: String? = if (tagklass.isInstance(event)) {
                // use reflection to get the id field named "<tagKlass>Id"
                val propertyName = tagklass.simpleName?.replaceFirstChar { it.lowercase() }
                try {
                    val eventClass = event::class
                    val properties = eventClass.memberProperties
                    val property = properties.find { it.name == propertyName }
                    @Suppress("UNCHECKED_CAST")
                    val stringProperty = property as? KProperty1<Any, String>
                    stringProperty?.get(event)
                } catch (e: Exception) {
                    null
                }
            } else {
                return@lastOrNull false
            }

            typedId == id
        }
    }
}

/**
 * Metrics data class for monitoring event store performance.
 */
data class EventStoreMetrics(
    val totalStored: Long,
    val totalPublished: Long,
    val pendingInOutbox: Long
)