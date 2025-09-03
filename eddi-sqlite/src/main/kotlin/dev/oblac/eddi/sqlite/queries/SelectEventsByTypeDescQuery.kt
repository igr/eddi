package dev.oblac.eddi.sqlite.queries

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.sqlite.dataToEventEnvelope
import dev.oblac.eddi.sqlite.mapToEventEnvelopeData
import java.sql.Connection

/**
 * SQL query object for selecting events by type in descending order.
 */
object SelectEventsByTypeDescQuery {
    const val SQL = """
        SELECT sequence, correlation_id, event_type, event_json, history_json, timestamp
        FROM event_envelopes 
        WHERE event_type = ?
        ORDER BY sequence DESC
    """
    
    operator fun invoke(connection: Connection, eventTypeName: String): List<EventEnvelope<Event>> {
        return connection.prepareStatement(SQL).use { stmt ->
            stmt.setString(1, eventTypeName)
            val rs = stmt.executeQuery()
            val candidates = mutableListOf<EventEnvelope<Event>>()
            while (rs.next()) {
                val data = rs.mapToEventEnvelopeData()
                candidates.add(dataToEventEnvelope(data))
            }
            candidates
        }
    }
}