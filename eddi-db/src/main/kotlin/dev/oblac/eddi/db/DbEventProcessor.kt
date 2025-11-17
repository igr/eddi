package dev.oblac.eddi.db

import dev.oblac.eddi.EventListener
import kotlinx.coroutines.*

class DbEventProcessor(private val processorId: Long) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startInbox(eventListener: EventListener) {
        scope.launch {
            while (true) {
                val newEvents = dbFetchLastUnpublishedEvents(processorId)
                for (event in newEvents.events) {
                    println("Dispatching event #${event.sequence}: ${event.event}")
                    eventListener(event)
                    dbMarkEventAsPublished(processorId, event.sequence)
                }

                delay(100L)
            }
        }
    }
}