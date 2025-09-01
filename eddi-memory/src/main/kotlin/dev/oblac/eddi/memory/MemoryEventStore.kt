package dev.oblac.eddi.memory

import dev.oblac.eddi.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
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
class MemoryEventStore: EventStore {

    // Persistent storage for all events (ordered by insertion)
    private val storedEvents = mutableListOf<EventEnvelope<Event>>()
    private val storageMutex = Mutex()

    // Metrics and tracking
    private val totalEventsStored = AtomicLong(0)

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

    override fun totalEventsStored(): Long = totalEventsStored.get()

    override fun findLast(fromIndex: Int): List<EventEnvelope<Event>> = storedEvents.subList(fromIndex, storedEvents.size)

    override fun findLastTaggedEvent(eventType: EventType, tag: Tag): EventEnvelope<Event>? {
        return storedEvents.lastOrNull {
            if (it.eventType != eventType) {
                return@lastOrNull false
            }
            val event = it.event
            val tagClass = tag::class

            // Find event property that is of the same type as the tag implementation
            val eventProperties = event::class.memberProperties
            val tagProperty = eventProperties.find { property ->
                property.returnType.classifier == tagClass
            }

            if (tagProperty != null) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val tagPropertyValue = (tagProperty as KProperty1<Any, Tag>).get(event)
                    return@lastOrNull tagPropertyValue == tag
                } catch (e: Exception) {
                    return@lastOrNull false
                }
            }

            false
        }
    }
}