package dev.oblac.eddi.memory

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventBus
import dev.oblac.eddi.EventEnvelope
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * todo extract interface for outbox and have a no-op impl for cases where outbox is not needed
 * Outbox implementation for MemoryEventStore that handles asynchronous event publishing.
 * 
 * This class implements the outbox pattern by:
 * - Tracking the last published event index
 * - Processing unpublished events asynchronously
 * - Maintaining event ordering through sequential processing
 * - Providing thread-safe access to outbox state
 */
class EventStoreOutbox(
    private val eventBus: EventBus,
    private val eventStore: MemoryEventStore,
    private val processingDelayMs: Long = 100L
) {
    
    // Index tracking the last published event (outbox pointer)
    private val lastPublishedIndex = AtomicLong(-1)
    
    // Processing control
    private val isProcessing = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var processingJob: Job = Job()
    
    // Metrics tracking
    private val totalEventsPublished = AtomicLong(0)
    
    /**
     * Starts the outbox processing.
     */
    fun start() {
        if (!isProcessing.compareAndSet(false, true)) {
            println("EventStoreOutbox is already processing")
            return
        }
        
        println("Starting EventStoreOutbox processing...")
        
        processingJob = scope.launch {
            try {
                while (isActive && isProcessing.get()) {
                    processOutboxEvents()
                    delay(processingDelayMs)
                }
            } catch (e: Exception) {
                println("Error in outbox processing: ${e.message}")
                // Continue processing unless cancelled
                if (e !is CancellationException) {
                    println("Restarting outbox processing after error...")
                    delay(1000) // Wait before restart
                    start() // Restart processing
                }
            }
        }
    }
    
    /**
     * Stops the outbox processing.
     */
    fun stop() {
        println("Stopping EventStoreOutbox...")
        isProcessing.set(false)
        processingJob.cancel()
        scope.cancel()
    }
    
    /**
     * Gets the total number of events published by the outbox.
     */
    fun getTotalEventsPublished(): Long = totalEventsPublished.get()
    
    /**
     * Gets the pending events count (difference between stored and published).
     */
    fun getPendingEventsCount(): Long {
        val storedCount = eventStore.totalEventsStored()
        return storedCount - totalEventsPublished.get()
    }
    
    /**
     * Processes events using index-based outbox pattern.
     * Compares lastPublishedIndex with storedEvents to find unpublished events.
     */
    private fun processOutboxEvents() {
        val lastPublished = lastPublishedIndex.get()
        val eventsToProcess = mutableListOf<EventEnvelope<Event>>()
        
        // Get unpublished events using the new getLast method
        val totalStored = eventStore.totalEventsStored()
        if (lastPublished + 1 < totalStored) {
            // Get events that need to be published (from lastPublished+1 to end)
            val startIndex = (lastPublished + 1)
            eventsToProcess.addAll(eventStore.findLast(startIndex.toInt()))
        }

        if (eventsToProcess.isEmpty()) {
            return
        }
        
        // Process events sequentially to maintain ordering
        var processedCount = 0
        for (eventEnvelope in eventsToProcess) {
            try {
                publishEvent(eventEnvelope)
                totalEventsPublished.incrementAndGet()
                processedCount++
                // Update the index after successful publication
                lastPublishedIndex.set(lastPublished + processedCount)
            } catch (e: Exception) {
                println("Failed to publish event at index ${lastPublished + processedCount + 1}: $eventEnvelope, error: ${e.message}")
                // Stop processing to maintain event ordering - failed event will be retried next time
                break
            }
        }
        
        if (processedCount > 0) {
            println("Processed $processedCount events from outbox (index-based)")
        }
    }

    fun publishEvent(eventEnvelope: EventEnvelope<Event>) {
        try {
            eventBus.publishEvent(eventEnvelope)
            println("Event published successfully: $eventEnvelope")
        } catch (e: Exception) {
            println("Failed to publish event: $eventEnvelope, error: ${e.message}")
            // In a real implementation, you might want to retry or store failed events
            throw e
        }
    }

}