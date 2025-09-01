package dev.oblac.eddi.memory

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventBus
import dev.oblac.eddi.EventEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

// todo extract eventHandler function signature

class MemoryEventBus : EventBus {
    private val eventChannel = Channel<EventEnvelope<Event>>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.Default)
    private val handlers = mutableMapOf<KClass<out Event>, (EventEnvelope<Event>) -> Array<Event>>()

    override fun start() {
        scope.launch {
            eventChannel.receiveAsFlow().collect { eventEnvelope ->
                handleEvent(eventEnvelope)
            }
        }
    }

    override fun <E : Event> registerEventHandler(eventClass: KClass<E>, handler: (EventEnvelope<E>) -> Array<Event>) {
        handlers[eventClass] = handler as (EventEnvelope<Event>) -> Array<Event>
    }

    override fun publishEvent(event: EventEnvelope<Event>) {
        eventChannel.trySend(event)
        println("Published event: $event")
    }

    override fun handleEvent(eventEnvelope: EventEnvelope<Event>) {
        println("Handling event: $eventEnvelope")
        val eventClass = eventEnvelope.event::class
        val handler = handlers[eventClass]
        if (handler != null) {
            handler(eventEnvelope).forEach { resultEvent ->
                val newEventEnvelope = EventEnvelope(eventEnvelope.correlationId, resultEvent)
                publishEvent(newEventEnvelope)
            }
        }
    }
}