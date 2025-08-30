package dev.oblac.eddi.memory

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventBus
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant

class MemoryEventStore(
    private val eventBus: EventBus
) : EventStore {
    private val eventChannel = Channel<EventEnvelope<Event>>(Channel.UNLIMITED)
    private val eventFlow: Flow<EventEnvelope<Event>> = eventChannel.receiveAsFlow()
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun storeEvents(correlationId: Long, events: Array<Event>): Array<EventEnvelope<Event>> {
        return events.map { event ->
            EventEnvelope(
                id = correlationId,
                event = event,
                timestamp = Instant.now(),
            ).also { envelope ->
                val result = eventChannel.trySend(envelope)
                if (result.isFailure) {
                    println("Failed to store event: ${result.exceptionOrNull()}")
                }
                println("Storing event: $envelope")
            }
        }.toTypedArray()
    }

    override fun publishEvent(eventEnvelope: EventEnvelope<Event>) {
        eventBus.publishEvent(eventEnvelope)
    }

    override fun start() {
        scope.launch {
            eventFlow.collect { eventEnvelope ->
                println("Processing event: $eventEnvelope")
                publishEvent(eventEnvelope)
            }
        }
    }

}