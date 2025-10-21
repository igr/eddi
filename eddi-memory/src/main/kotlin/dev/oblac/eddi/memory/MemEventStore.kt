package dev.oblac.eddi.memory

import dev.oblac.eddi.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class MemEventStore : EventStoreInbox, EventStoreRepo {

    private val atomicCounter = AtomicLong(0)

    override fun <E : Event> storeEvent(correlationId: Long, event: E): EventEnvelope<E> {
        val ee = EventEnvelope(
            sequence = atomicCounter.incrementAndGet(),
            correlationId = correlationId,
            event = event,
        )
        storedEvents.add(ee as EventEnvelope<Event>)
        return ee
    }

    private val lastEventIndex = AtomicLong(0)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startInbox(dispatch: EventListener) {
        scope.launch {
            while (true) {
                val currentIndex = lastEventIndex.get()
                val newEvents = findLastEvents(currentIndex.toInt())
                for (event in newEvents) {
                    println("MemEventStore: dispatching event #${event.sequence}: ${event.event}")
                    dispatch(event)
                    lastEventIndex.incrementAndGet()
                }
                kotlinx.coroutines.delay(100L)
            }
        }
    }

    /**
     * In-memory storage for all events (ordered by insertion)!
     */
    internal val storedEvents = ConcurrentLinkedDeque<EventEnvelope<Event>>()       // maybe use ConcurrentLinkedQueue?

    override fun totalEventsStored(): Long = storedEvents.size.toLong()

    override fun findLastEvents(fromIndex: Int): Sequence<EventEnvelope<Event>> =
        storedEvents
            .filterIndexed { index, _ -> index >= fromIndex }
            .asSequence()

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

            if (tagProperty == null) {
                return@lastOrNull false
            }
            @Suppress("UNCHECKED_CAST")
            val tagPropertyValue = (tagProperty as KProperty1<Any, Tag>).get(event)
            tagPropertyValue == tag
        }
    }

    // todo return Event?
    override fun findLastTaggedEvent(eventType: EventType): EventEnvelope<Event>? {
        return storedEvents.filterIndexed { index, _ -> index <= lastEventIndex.get() }.lastOrNull { it.eventType == eventType }
    }

    override fun findLastTaggedEvent(tag: Tag): EventEnvelope<Event>? {
        return storedEvents.lastOrNull {
            val event = it.event
            val tagClass = tag::class

            // Find event property that is of the same type as the tag implementation
            val eventProperties = event::class.memberProperties
            val tagProperty = eventProperties.find { property ->
                property.returnType.classifier == tagClass
            }

            if (tagProperty == null) {
                return@lastOrNull false
            }

            @Suppress("UNCHECKED_CAST")
            val tagPropertyValue = (tagProperty as KProperty1<Any, Tag>).get(event)
            tagPropertyValue == tag
        }
    }
}