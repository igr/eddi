package dev.oblac.eddi.sqlite.queries

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.Tag
import dev.oblac.eddi.sqlite.dataToEventEnvelope
import dev.oblac.eddi.sqlite.mapToEventEnvelopeData
import java.sql.Connection

/**
 * OPTIMIZE!!!!!!!!!!!!!!!!!!
 */
object FindLastEventByTagDescQuery {
    const val SQL = """
        SELECT sequence, correlation_id, event_type, event_json, history_json, tags_json, timestamp
        FROM event_envelopes EE, json_each(EE.tags_json) AS T 
        WHERE T.value=?
        ORDER BY sequence DESC
        LIMIT 1
    """
    operator fun invoke(connection: Connection, tag: Tag): EventEnvelope<Event>? {
        return connection.prepareStatement(SQL).use { stmt ->
            stmt.setString(1, tag.id)
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