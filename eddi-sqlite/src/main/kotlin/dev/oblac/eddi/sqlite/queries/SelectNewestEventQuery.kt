package dev.oblac.eddi.sqlite.queries

import java.sql.Connection
import java.time.Instant

/**
 * SQL query object for selecting the newest event timestamp.
 * Used for database statistics.
 */
object SelectNewestEventQuery {
    const val SQL = """
        SELECT timestamp
        FROM event_envelopes 
        ORDER BY timestamp DESC
        LIMIT 1
    """
    
    operator fun invoke(connection: Connection): Instant? {
        return connection.prepareStatement(SQL).use { stmt ->
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getTimestamp(1).toInstant() else null
        }
    }
}