package dev.oblac.eddi.eventbus

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class EventBusMemory : EventBus {
    private val eventChannel = Channel<EventEnvelope<Event>>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.Default)
    private val handlers = mutableListOf<(EventEnvelope<Event>) -> Unit>()

    override fun start() {
        scope.launch {
            eventChannel.receiveAsFlow().collect { eventEnvelope ->
                handleEvent(eventEnvelope)
            }
        }
    }

    override fun registerEventHandler(handler: (EventEnvelope<Event>) -> Unit) {
        handlers.add(handler)
    }

    override fun publishEvent(event: EventEnvelope<Event>) {
        eventChannel.trySend(event)
        println("Published event: $event")
    }

    override fun handleEvent(eventEnvelope: EventEnvelope<Event>) {
        println("Handling event: $eventEnvelope")
        handlers.forEach { handler ->
            handler(eventEnvelope)
        }
    }
}