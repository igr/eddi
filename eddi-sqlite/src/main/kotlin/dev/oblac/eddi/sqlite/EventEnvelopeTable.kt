package dev.oblac.eddi.sqlite

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * SQLite table for storing EventEnvelope data with optimized indexing.
 */
object EventEnvelopeTable : Table("event_envelopes") {
    val sequence = long("sequence").autoIncrement()
    val correlationId = long("correlation_id")
    val eventType = varchar("event_type", 255)
    val eventJson = text("event_json")
    val historyJson = text("history_json")
    val timestamp = timestamp("timestamp")
    
    override val primaryKey = PrimaryKey(sequence)
    
    init {
        // Index for finding events from a specific index (used in findLastEvent)
        index(false, sequence)
        
        // Index for event type queries (used in findLastTaggedEvent with eventType)
        index(false, eventType)
        
        // Index for timestamp-based queries and ordering
        index(false, timestamp)
        
        // Composite index for event type + timestamp for optimal tag-based queries
        index(false, eventType, timestamp)
        
        // Index for correlation ID queries
        index(false, correlationId)
    }
}