package dev.oblac.eddi.sqlite.queries

import java.sql.Connection

/**
 * SQL query object for counting all events in the database.
 */
object CountAllEventsQuery {
    const val SQL = """
        SELECT COUNT(*) FROM event_envelopes
    """
    
    operator fun invoke(connection: Connection): Long {
        return connection.prepareStatement(SQL).use { stmt ->
            val rs = stmt.executeQuery()
            rs.next()
            rs.getLong(1)
        }
    }
}

