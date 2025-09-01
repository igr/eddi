package dev.oblac.eddi.memory

import dev.oblac.eddi.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class MemoryEventStoreRepo(
    private val es: MemoryEventStore
) : EventStoreRepo {

    private val storedEvents = es.storedEvents

    override fun totalEventsStored(): Long = es.totalEventsStored.get()

    override fun findLastEvent(fromIndex: Int): List<EventEnvelope<Event>> = storedEvents.subList(fromIndex, storedEvents.size)

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