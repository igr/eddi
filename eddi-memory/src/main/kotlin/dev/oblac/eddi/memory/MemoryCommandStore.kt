package dev.oblac.eddi.memory

import dev.oblac.eddi.Command
import dev.oblac.eddi.CommandBus
import dev.oblac.eddi.CommandEnvelope
import dev.oblac.eddi.CommandStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * Memory-based command store implementation.
 * 
 * This implementation:
 * - Stores commands persistently in memory using thread-safe collections
 * - Delegates outbox pattern functionality to CommandStoreOutbox
 * - Maintains command ordering and provides retrieval capabilities
 * - Memory efficient: commands stored only once, no separate outbox queue
 */
class MemoryCommandStore(
    private val commandBus: CommandBus,
    private val processingDelayMs: Long = 100L // Configurable delay for outbox processing
) : CommandStore {
    
    // Persistent storage for all commands (ordered by insertion)
    private val storedCommands = mutableListOf<CommandEnvelope<Command>>()
    private val storageMutex = Mutex()
    
    // Outbox for handling asynchronous command publishing
    private lateinit var outbox: CommandStoreOutbox
    
    // Metrics and tracking
    private val totalCommandsStored = AtomicLong(0)

    override fun <T : Command> storeCommand(command: T): CommandEnvelope<T> {
        return runBlocking {
            val envelope = CommandEnvelope(
                id = System.currentTimeMillis(),
                command = command,
                timestamp = Instant.now()
            )
            
            // Store command persistently (outbox pattern - store first)
            storageMutex.withLock {
                storedCommands.add(envelope as CommandEnvelope<Command>)
                totalCommandsStored.incrementAndGet()
            }
            
            // Command is now available for outbox processing via index comparison
            //println("Command stored for processing: $envelope")
            
            envelope
        }
    }

    override fun start() {
        println("Starting MemoryCommandStore...")
        
        // Initialize and start the outbox
        outbox = CommandStoreOutbox(commandBus, this, processingDelayMs)
        outbox.start()
        
        println("MemoryCommandStore started with outbox processing")
    }
    
    /**
     * Stops the command processing.
     */
    fun stop() {
        println("Stopping MemoryCommandStore...")
        if (::outbox.isInitialized) {
            outbox.stop()
        }
    }
    
    /**
     * Retrieves all stored commands, optionally filtered by correlation ID.
     */
    suspend fun getStoredCommands(correlationId: Long? = null): List<CommandEnvelope<Command>> {
        return storageMutex.withLock {
            if (correlationId != null) {
                storedCommands.filter { it.id == correlationId }
            } else {
                storedCommands.toList() // Return a copy to avoid concurrent modification
            }
        }
    }
    
    /**
     * Retrieves commands stored after a specific timestamp.
     */
    suspend fun getCommandsAfter(timestamp: Instant): List<CommandEnvelope<Command>> {
        return storageMutex.withLock {
            storedCommands.filter { it.timestamp.isAfter(timestamp) }
        }
    }
    
    /**
     * Gets the total number of commands stored and published.
     */
    fun metrics(): CommandStoreMetrics {
        val storedCount = totalCommandsStored.get()
        val publishedCount = if (::outbox.isInitialized) outbox.getTotalCommandsPublished() else 0L
        val pendingCount = if (::outbox.isInitialized) outbox.getPendingCommandsCount() else 0L
        return CommandStoreMetrics(
            totalStored = storedCount,
            totalPublished = publishedCount,
            pendingInOutbox = pendingCount
        )
    }
    
    /**
     * Internal method for outbox to access storage mutex.
     * Package-private for use by CommandStoreOutbox only.
     */
    internal fun getStorageMutex(): Mutex = storageMutex
    
    /**
     * Internal method for outbox to access stored commands.
     * Package-private for use by CommandStoreOutbox only.
     */
    internal fun getStoredCommandsInternal(): List<CommandEnvelope<Command>> = storedCommands
    
    /**
     * Internal method for outbox to get total commands stored count.
     * Package-private for use by CommandStoreOutbox only.
     */
    internal fun getTotalCommandsStored(): Long = totalCommandsStored.get()
}

/**
 * Metrics data class for monitoring command store performance.
 */
data class CommandStoreMetrics(
    val totalStored: Long,
    val totalPublished: Long,
    val pendingInOutbox: Long
)
