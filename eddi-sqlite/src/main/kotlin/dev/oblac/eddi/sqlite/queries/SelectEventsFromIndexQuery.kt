package dev.oblac.eddi.sqlite.queries

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.sqlite.dataToEventEnvelope
import dev.oblac.eddi.sqlite.mapToEventEnvelopeData
import java.sql.Connection

/**
 * SQL query object for selecting events from a specific index onwards.
 */
object SelectEventsFromIndexQuery {
    const val SQL = """
        SELECT sequence, correlation_id, event_type, event_json, history_json, timestamp
        FROM event_envelopes 
        WHERE sequence > ?
        ORDER BY sequence ASC
    """
    
    operator fun invoke(connection: Connection, fromIndex: Int): List<EventEnvelope<Event>> {
        return connection.prepareStatement(SQL).use { stmt ->
            stmt.setLong(1, fromIndex.toLong())
            val rs = stmt.executeQuery()
            val events = mutableListOf<EventEnvelope<Event>>()
            while (rs.next()) {
                val data = rs.mapToEventEnvelopeData()
                events.add(dataToEventEnvelope(data))
            }
            events
        }
    }
}