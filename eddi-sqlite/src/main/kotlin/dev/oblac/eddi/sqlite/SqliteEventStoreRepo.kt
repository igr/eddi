package dev.oblac.eddi.sqlite

import dev.oblac.eddi.*
import dev.oblac.eddi.sqlite.queries.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * SQLite-based implementation of EventStoreRepo using JDBC.
 */
class SqliteEventStoreRepo(
    private val databasePath: String = "event_store.db"
) : EventStoreRepo {

    private val database: JdbcDatabase
    private val initMutex = Mutex()
    private var isInitialized = false

    init {
        // Initialize SQLite database connection
        database = JdbcDatabase(
            url = "jdbc:sqlite:$databasePath"
        )

        // Configure SQLite for performance
        //configureSqliteForPerformance()

        runBlocking {
            ensureInitialized()
        }

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
                    SqliteSchema.createSchema(database)
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
        try {
            database.transaction { connection ->
                InsertEventStatement(connection, envelope)
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
            runBlocking {
                database.transaction { connection ->
                    connection.prepareStatement(CountAllEventsQuery.SQL).use { stmt ->
                        val rs = stmt.executeQuery()
                        rs.next()
                        rs.getLong(1)
                    }
                }
            }
        } catch (e: Exception) {
            throw SqliteEventStoreException("Failed to count total events", e)
        }
    }

    override fun findLastEvents(fromIndex: Int): List<EventEnvelope<Event>> {
        return try {
            runBlocking {
                database.transaction { connection ->
                    SelectEventsFromIndexQuery(connection, fromIndex)
                }
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
            runBlocking {
                database.transaction { connection ->
                    val candidates = SelectEventsByTypeDescQuery(connection, eventType.name)
                    // Filter by tag using reflection (same logic as MemoryEventStoreRepo)
                    candidates.firstOrNull { envelope ->
                        hasMatchingTag(envelope.event, tag)
                    }
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
            runBlocking {
                database.transaction { connection ->
                    val candidates = SelectAllEventsDescQuery(connection)

                    // Filter by tag using reflection
                    candidates.firstOrNull { envelope ->
                        hasMatchingTag(envelope.event, tag)
                    }
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
        return runBlocking {
            database.transaction { connection ->
                val query = """
                    SELECT sequence, correlation_id, event_type, event_json, history_json, timestamp
                    FROM event_envelopes 
                    WHERE event_type = ? AND event_json LIKE ?
                    ORDER BY sequence DESC
                    LIMIT 1
                """

                connection.prepareStatement(query).use { stmt ->
                    stmt.setString(1, eventType.name)
                    stmt.setString(2, "%\"$tagId\"%")
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        val data = rs.mapToEventEnvelopeData()
                        dataToEventEnvelope(data)
                    } else {
                        null
                    }
                }
            }
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
        return runBlocking {
            database.transaction { connection ->
                val totalEvents = CountAllEventsQuery(connection)

                val oldestEvent = SelectOldestEventQuery(connection)

                val newestEvent = SelectNewestEventQuery(connection)

                DatabaseStats(
                    totalEvents = totalEvents,
                    oldestEventTimestamp = oldestEvent,
                    newestEventTimestamp = newestEvent,
                    databasePath = databasePath
                )
            }
        }
    }

    /**
     * Closes the database connection pool.
     * Should be called during application shutdown.
     */
    fun close() {
        database.close()
    }

    /**
     * Configures SQLite for performance without using Exposed.
     */
    private fun configureSqliteForPerformance() {
        val commands = listOf(
            "PRAGMA journal_mode=WAL",
            "PRAGMA cache_size=10000",
            "PRAGMA foreign_keys=OFF",
            "PRAGMA temp_store=MEMORY",
            "PRAGMA optimize",
            "PRAGMA busy_timeout=30000", // 30 seconds
            "PRAGMA mmap_size=268435456" // 256MB
        )
        commands.forEach {
            println("Executing command: $it")
            database.connection().createStatement().executeQuery(it).close()
        }

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