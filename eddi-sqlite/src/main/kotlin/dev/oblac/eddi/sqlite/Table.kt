package dev.oblac.eddi.sqlite

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventType
import java.sql.ResultSet
import java.time.Instant

/**
 * Maps a ResultSet row to EventEnvelope data class.
 */
fun ResultSet.mapToEventEnvelopeData(): EventEnvelopeData {
    return EventEnvelopeData(
        sequence = this.getLong("sequence"),
        correlationId = this.getLong("correlation_id"),
        eventType = this.getString("event_type"),
        eventJson = this.getString("event_json"),
        historyJson = this.getString("history_json"),
        timestamp = this.getTimestamp("timestamp").toInstant()
    )
}

/**
 * Converts database row data to an EventEnvelope.
 */

fun dataToEventEnvelope(data: EventEnvelopeData): EventEnvelope<Event> {
    return try {
        EventEnvelope(
            sequence = data.sequence,
            correlationId = data.correlationId,
            event = JsonUtils.deserializeEvent(data.eventJson),
            eventType = EventType(data.eventType),
            history = JsonUtils.deserializeHistory(data.historyJson),
            timestamp = data.timestamp
        )
    } catch (e: Exception) {
        throw SqliteEventStoreException(
            "Failed to deserialize event envelope from database row. Event JSON: ${data.eventJson}",
            e
        )
    }
}



/**
 * Data class representing raw event envelope data from database.
 */
data class EventEnvelopeData(
    val sequence: Long,
    val correlationId: Long,
    val eventType: String,
    val eventJson: String,
    val historyJson: String,
    val timestamp: Instant
)