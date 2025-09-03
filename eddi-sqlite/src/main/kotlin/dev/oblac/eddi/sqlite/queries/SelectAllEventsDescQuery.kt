package dev.oblac.eddi.sqlite.queries

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.sqlite.dataToEventEnvelope
import dev.oblac.eddi.sqlite.mapToEventEnvelopeData
import java.sql.Connection

/**
 * SQL query object for selecting all events in descending order.
 * Note: This query should be used with caution for large datasets and
 * consider implementing a streaming version for regular services.
 */
object SelectAllEventsDescQuery {
    const val SQL = """
        SELECT sequence, correlation_id, event_type, event_json, history_json, timestamp
        FROM event_envelopes 
        ORDER BY sequence DESC
    """

    operator fun invoke(connection: Connection): List<EventEnvelope<Event>> {
        return connection.prepareStatement(SQL).use { stmt ->
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