package dev.oblac.eddi.memory

import dev.oblac.eddi.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class MemoryEventStoreRepo : EventStoreRepo {

    /**
     * In-memory storage for all events (ordered by insertion)!
     */
    internal val storedEvents = mutableListOf<EventEnvelope<Event>>()       // maybe use ConcurrentLinkedQueue?

    override fun totalEventsStored(): Long = storedEvents.size.toLong()

    override fun findLastEvents(fromIndex: Int): List<EventEnvelope<Event>> = storedEvents.subList(fromIndex, storedEvents.size)

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
            try {
                @Suppress("UNCHECKED_CAST")
                val tagPropertyValue = (tagProperty as KProperty1<Any, Tag>).get(event)
                tagPropertyValue == tag
            } catch (e: Exception) {
                false
            }
        }
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

            try {
                @Suppress("UNCHECKED_CAST")
                val tagPropertyValue = (tagProperty as KProperty1<Any, Tag>).get(event)
                tagPropertyValue == tag
            } catch (e: Exception) {
                false
            }
        }
    }
}