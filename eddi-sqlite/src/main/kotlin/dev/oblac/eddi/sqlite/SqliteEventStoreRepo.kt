package dev.oblac.eddi.sqlite

import dev.oblac.eddi.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * SQLite-based implementation of EventStoreRepo using Exposed ORM.
 */
class SqliteEventStoreRepo(
    private val databasePath: String = "event_store.db"
) : EventStoreRepo {
    
    private val database: Database
    private val initMutex = Mutex()
    private var isInitialized = false
    
    init {
        // Initialize SQLite database connection
        database = Database.connect(
            url = "jdbc:sqlite:$databasePath",
            driver = "org.sqlite.JDBC"
        )

        runBlocking {
            ensureInitialized()
        }

        //configureSqliteForPerformance(database)
    }
    
    /**
     * Ensures database schema is created.
     * Thread-safe initialization that runs only once.
     */
    private suspend fun ensureInitialized() {
        if (isInitialized) return
        initMutex.withLock {
            if (!isInitialized) {
                try {
                    TransactionUtils.safeTransaction(database) {
                        SchemaUtils.create(EventEnvelopeTable)
                    }
                    isInitialized = true
                } catch (e: Exception) {
                    throw SqliteEventStoreException(
                        "Failed to initialize database schema",
                        e
                    )
                }
            }
        }
    }
    
    /**
     * Internal method to store an event envelope.
     * Used by EventStore implementations.
     */
    suspend fun storeEventEnvelope(envelope: EventEnvelope<Event>) {
        ensureInitialized()
        
        try {
            TransactionUtils.safeTransaction(database) {
                EventEnvelopeTable.insert {
                    it[sequence] = envelope.sequence
                    it[correlationId] = envelope.correlationId
                    it[eventType] = envelope.eventType.name
                    it[eventJson] = JsonUtils.serializeEvent(envelope.event)
                    it[historyJson] = JsonUtils.serializeHistory(envelope.history)
                    it[timestamp] = envelope.timestamp
                }
            }
        } catch (e: Exception) {
            throw SqliteEventStoreException(
                "Failed to store event envelope with sequence ${envelope.sequence}",
                e
            )
        }
    }
    
    override fun totalEventsStored(): Long {
        return try {
            TransactionUtils.safeTransaction(database) {
                EventEnvelopeTable.selectAll().count()
            }
        } catch (e: Exception) {
            throw SqliteEventStoreException("Failed to count total events", e)
        }
    }
    
    override fun findLastEvents(fromIndex: Int): List<EventEnvelope<Event>> {
        return try {
            TransactionUtils.safeTransaction(database) {
                EventEnvelopeTable
                    .selectAll()
                    .where { EventEnvelopeTable.sequence greater fromIndex.toLong() }
                    .orderBy(EventEnvelopeTable.sequence, SortOrder.ASC)
                    .map { row -> rowToEventEnvelope(row) }
            }
        } catch (e: Exception) {
            throw SqliteEventStoreException(
                "Failed to find events from index $fromIndex",
                e
            )
        }
    }
    
    override fun findLastTaggedEvent(eventType: EventType, tag: Tag): EventEnvelope<Event>? {
        return try {
            TransactionUtils.safeTransaction(database) {
                // First, find all events of the specified type
                val candidates = EventEnvelopeTable
                    .selectAll()
                    .where { EventEnvelopeTable.eventType eq eventType.name }
                    .orderBy(EventEnvelopeTable.sequence, SortOrder.DESC)
                    .map { row -> rowToEventEnvelope(row) }
                
                // Then filter by tag using reflection (same logic as MemoryEventStoreRepo)
                candidates.firstOrNull { envelope ->
                    hasMatchingTag(envelope.event, tag)
                }
            }
        } catch (e: Exception) {
            throw SqliteEventStoreException(
                "Failed to find last tagged event of type ${eventType.name} with tag ${tag.id}",
                e
            )
        }
    }
    
    override fun findLastTaggedEvent(tag: Tag): EventEnvelope<Event>? {
        return try {
            TransactionUtils.safeTransaction(database) {
                // Get all events ordered by sequence descending for efficient search
                val candidates = EventEnvelopeTable
                    .selectAll()
                    .orderBy(EventEnvelopeTable.sequence, SortOrder.DESC)
                    .map { row -> rowToEventEnvelope(row) }
                
                // Filter by tag using reflection
                candidates.firstOrNull { envelope ->
                    hasMatchingTag(envelope.event, tag)
                }
            }
        } catch (e: Exception) {
            throw SqliteEventStoreException(
                "Failed to find last tagged event with tag ${tag.id}",
                e
            )
        }
    }
    
    /**
     * Optimized method to find the last tagged event with better SQL filtering.
     * This is an enhanced version that could be used for better performance
     * if the Event structure allows for more direct SQL querying.
     */
    fun findLastTaggedEventOptimized(eventType: EventType, tagId: String): EventEnvelope<Event>? {
        return transaction(database) {
            // Use LIKE query to search in JSON for better performance
            // This assumes the tag ID appears in the event JSON
            EventEnvelopeTable
                .selectAll()
                .where { 
                    (EventEnvelopeTable.eventType eq eventType.name) and
                    (EventEnvelopeTable.eventJson like "%\"$tagId\"%")
                }
                .orderBy(EventEnvelopeTable.sequence, SortOrder.DESC)
                .limit(1)
                .map { row -> rowToEventEnvelope(row) }
                .firstOrNull()
        }
    }

    /**
     * Converts a database row to an EventEnvelope.
     */
    private fun rowToEventEnvelope(row: ResultRow): EventEnvelope<Event> {
        return try {
            EventEnvelope(
                sequence = row[EventEnvelopeTable.sequence],
                correlationId = row[EventEnvelopeTable.correlationId],
                event = JsonUtils.deserializeEvent(row[EventEnvelopeTable.eventJson]),
                eventType = EventType(row[EventEnvelopeTable.eventType]),
                history = JsonUtils.deserializeHistory(row[EventEnvelopeTable.historyJson]),
                timestamp = row[EventEnvelopeTable.timestamp]
            )
        } catch (e: Exception) {
            throw SqliteEventStoreException(
                "Failed to deserialize event envelope from database row. Event JSON: ${row[EventEnvelopeTable.eventJson]}",
                e
            )
        }
    }
    
    /**
     * Checks if an event has a matching tag using reflection.
     * This mirrors the logic from MemoryEventStoreRepo.
     * todo REMOVE!
     */
    private fun hasMatchingTag(event: Event, tag: Tag): Boolean {
        val tagClass = tag::class
        
        // Find event property that is of the same type as the tag implementation
        val eventProperties = event::class.memberProperties
        val tagProperty = eventProperties.find { property ->
            property.returnType.classifier == tagClass
        }
        
        if (tagProperty == null) {
            return false
        }
        
        return try {
            @Suppress("UNCHECKED_CAST")
            val tagPropertyValue = (tagProperty as KProperty1<Any, Tag>).get(event)
            tagPropertyValue == tag
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Provides database statistics for monitoring and debugging.
     */
    fun calculateDatabaseStats(): DatabaseStats {
        return transaction(database) {
            val totalEvents = EventEnvelopeTable.selectAll().count()
            val oldestEvent = EventEnvelopeTable
                .selectAll()
                .orderBy(EventEnvelopeTable.timestamp, SortOrder.ASC)
                .limit(1)
                .map { it[EventEnvelopeTable.timestamp] }
                .firstOrNull()
            val newestEvent = EventEnvelopeTable
                .selectAll()
                .orderBy(EventEnvelopeTable.timestamp, SortOrder.DESC)
                .limit(1)
                .map { it[EventEnvelopeTable.timestamp] }
                .firstOrNull()
                
            DatabaseStats(
                totalEvents = totalEvents,
                oldestEventTimestamp = oldestEvent,
                newestEventTimestamp = newestEvent,
                databasePath = databasePath
            )
        }
    }
    
    /**
     * Closes the database connection.
     * Should be called during application shutdown.
     */
    fun close() {
        // Exposed automatically manages connection pooling and closing
        // No explicit close needed, but this method is provided for completeness
    }
}

/**
 * Data class containing database statistics.
 */
data class DatabaseStats(
    val totalEvents: Long,
    val oldestEventTimestamp: Instant?,
    val newestEventTimestamp: Instant?,
    val databasePath: String
)