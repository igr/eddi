package dev.oblac.eddi.db

import dev.oblac.eddi.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class DbEventStore : EventStoreInbox, EventStoreRepo {

    override fun <E : Event> storeEvent(correlationId: ULong, event: E): EventEnvelope<E> {
        return dbStoreEvent(correlationId, event)
    }

    private val lastEventIndex = AtomicLong(0)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startInbox(eventListener: EventListener) {
        scope.launch {
            while (true) {
                val newEvents = fetchLastUnpublishedEvents()
                for (event in newEvents.events) {
                    println("Dispatching event #${event.sequence}: ${event.event}")
                    eventListener(event)
                    dbMarkEventAsPublished(event.sequence)
                }

                delay(100L)
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

    override fun findLastTaggedEvent(eventName: EventName, tag: Tag): EventEnvelope<Event>? {
        return storedEvents.lastOrNull {
            if (it.eventName != eventName) {
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
    override fun findLastTaggedEvent(eventName: EventName): EventEnvelope<Event>? {
        return storedEvents.filterIndexed { index, _ -> index <= lastEventIndex.get() }.lastOrNull { it.eventName == eventName }
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