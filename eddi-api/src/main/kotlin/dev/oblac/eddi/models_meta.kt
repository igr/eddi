package dev.oblac.eddi

import kotlin.reflect.KClass

/**
 * Registry of event classes. Events are stored under their [EventName] and read back by it,
 * so every event class must be registered before it is stored.
 */
object Events {
    private val nameToClass = mutableMapOf<EventName, KClass<out Event>>()

    fun register(vararg events: KClass<out Event>) {
        events.forEach { nameToClass[EventName.of(it)] = it }
    }

    fun nameOf(event: Event): EventName {
        val name = EventName.of(event::class)
        if (name !in nameToClass) error("Event ${event::class.simpleName} is not registered")
        return name
    }

    fun classOf(name: EventName): KClass<out Event> =
        nameToClass[name] ?: error("Event with name '${name.value}' is not registered")
}
