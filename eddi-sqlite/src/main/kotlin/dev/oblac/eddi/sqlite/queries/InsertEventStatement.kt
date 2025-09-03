package dev.oblac.eddi.sqlite.queries

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.sqlite.JsonUtils
import java.sql.Connection
import java.sql.Timestamp

/**
 * SQL query object for inserting event envelopes into the database.
 */
object InsertEventStatement {
    const val SQL = """
        INSERT INTO event_envelopes (correlation_id, event_type, event_json, history_json, timestamp)
        VALUES (?, ?, ?, ?, ?)
    """
    
    operator fun invoke(connection: Connection, envelope: EventEnvelope<Event>) {
        connection.prepareStatement(SQL).use { stmt ->
            stmt.setLong(1, envelope.correlationId)
            stmt.setString(2, envelope.eventType.name)
            stmt.setString(3, JsonUtils.serializeEvent(envelope.event))
            stmt.setString(4, JsonUtils.serializeHistory(envelope.history))
            stmt.setTimestamp(5, Timestamp.from(envelope.timestamp))
            stmt.executeUpdate()
        }
    }
}