package dev.oblac.eddi.sqlite

import dev.oblac.eddi.EventStore
import dev.oblac.eddi.EventStoreRepo
import java.io.File

/**
 * Factory for creating SQLite-based Eddi components.
 * 
 * This factory provides convenient methods for setting up the SQLite
 * event store with proper configuration and error handling.
 */
object SqliteEddiFactory {
    
    /**
     * Creates a complete SQLite-based EventStore and EventStoreRepo pair.
     * 
     * @param databasePath Path to the SQLite database file
     * @param createDirectories Whether to create parent directories if they don't exist
     * @return Pair of (EventStore, EventStoreRepo)
     * @throws SqliteEventStoreException if database initialization fails
     */
    fun createEventStore(
        databasePath: String = "event_store.db",
        createDirectories: Boolean = true
    ): Pair<EventStore, EventStoreRepo> {
        return try {
            // Ensure parent directories exist if requested
            if (createDirectories) {
                val dbFile = File(databasePath)
                dbFile.parentFile?.mkdirs()
            }
            
            val repo = SqliteEventStoreRepo(databasePath)
            val store = SqliteEventStore(repo, databasePath)
            
            Pair(store, repo)
        } catch (e: Exception) {
            throw SqliteEventStoreException(
                "Failed to create SQLite Event Store at path: $databasePath", 
                e
            )
        }
    }
    
    /**
     * Creates an EventStoreRepo only (without EventStore).
     * Useful when you only need query capabilities.
     */
    fun createEventStoreRepo(
        databasePath: String = "event_store.db",
        createDirectories: Boolean = true
    ): EventStoreRepo {
        return try {
            if (createDirectories) {
                val dbFile = File(databasePath)
                dbFile.parentFile?.mkdirs()
            }
            
            SqliteEventStoreRepo(databasePath)
        } catch (e: Exception) {
            throw SqliteEventStoreException(
                "Failed to create SQLite Event Store Repository at path: $databasePath", 
                e
            )
        }
    }
    
    /**
     * Creates an in-memory SQLite database for testing purposes.
     * This database will be lost when the application terminates.
     */
    fun createInMemoryEventStore(): Pair<EventStore, EventStoreRepo> {
        return createEventStore(":memory:", false)
    }
}

/**
 * Custom exception for SQLite Event Store related errors.
 */
class SqliteEventStoreException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)