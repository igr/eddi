package dev.oblac.eddi.memory

import dev.oblac.eddi.Command
import dev.oblac.eddi.CommandBus
import dev.oblac.eddi.CommandEnvelope
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * todo extract interface for outbox and have a no-op impl for cases where outbox is not needed
 * Outbox implementation for MemoryCommandStore that handles asynchronous command publishing.
 * 
 * This class implements the outbox pattern by:
 * - Tracking the last published command index
 * - Processing unpublished commands asynchronously
 * - Maintaining command ordering through sequential processing
 * - Providing thread-safe access to outbox state
 */
class CommandStoreOutbox(
    private val commandBus: CommandBus,
    private val commandStore: MemoryCommandStore,
    private val processingDelayMs: Long = 100L
) {
    
    // Index tracking the last published command (outbox pointer)
    private val lastPublishedIndex = AtomicLong(-1)
    
    // Processing control
    private val isProcessing = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var processingJob: Job = Job()
    
    // Metrics tracking
    private val totalCommandsPublished = AtomicLong(0)
    
    /**
     * Starts the outbox processing.
     */
    fun start() {
        if (!isProcessing.compareAndSet(false, true)) {
            println("CommandStoreOutbox is already processing")
            return
        }
        
        println("Starting CommandStoreOutbox processing...")
        
        processingJob = scope.launch {
            try {
                while (isActive && isProcessing.get()) {
                    processOutboxCommands()
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
        println("Stopping CommandStoreOutbox...")
        isProcessing.set(false)
        processingJob.cancel()
        scope.cancel()
    }
    
    /**
     * Gets the total number of commands published by the outbox.
     */
    fun getTotalCommandsPublished(): Long = totalCommandsPublished.get()
    
    /**
     * Gets the pending commands count (difference between stored and published).
     */
    fun getPendingCommandsCount(): Long {
        val storedCount = commandStore.getTotalCommandsStored()
        return storedCount - totalCommandsPublished.get()
    }
    
    /**
     * Processes commands using index-based outbox pattern.
     * Compares lastPublishedIndex with storedCommands to find unpublished commands.
     */
    private suspend fun processOutboxCommands() {
        val lastPublished = lastPublishedIndex.get()
        val commandsToProcess = mutableListOf<CommandEnvelope<Command>>()
        
        // Get unpublished commands by comparing index with stored commands size
        // todo DONT HOLD LOCK WHILE PUBLISHING as we dont need to block storage during publishing
        // todo dont get all commands! just get one by one using the index
        commandStore.getStorageMutex().withLock {
            val storedCommands = commandStore.getStoredCommandsInternal()
            val totalStored = storedCommands.size
            if (lastPublished + 1 < totalStored) {
                // Copy commands that need to be published (from lastPublished+1 to end)
                val startIndex = (lastPublished + 1).toInt()
                commandsToProcess.addAll(storedCommands.subList(startIndex, totalStored))
            }
        }
        
        if (commandsToProcess.isEmpty()) {
            return
        }
        
        // Process commands sequentially to maintain ordering
        var processedCount = 0
        for (commandEnvelope in commandsToProcess) {
            try {
                publishCommand(commandEnvelope)
                totalCommandsPublished.incrementAndGet()
                processedCount++
                // Update the index after successful publication
                lastPublishedIndex.set(lastPublished + processedCount)
            } catch (e: Exception) {
                println("Failed to publish command at index ${lastPublished + processedCount + 1}: $commandEnvelope, error: ${e.message}")
                // Stop processing to maintain command ordering - failed command will be retried next time
                break
            }
        }
        
        if (processedCount > 0) {
            println("Processed $processedCount commands from outbox (index-based)")
        }
    }

    fun publishCommand(commandEnvelope: CommandEnvelope<Command>) {
        try {
            commandBus.publishCommand(commandEnvelope)
            println("Command published successfully: $commandEnvelope")
        } catch (e: Exception) {
            println("Failed to publish command: $commandEnvelope, error: ${e.message}")
            // In a real implementation, you might want to retry or store failed commands
            throw e
        }
    }

}