package dev.oblac.eddi.sqlite

object SqliteQueries {

    const val INSERT_EVENT = """
        INSERT INTO event_envelopes (correlation_id, event_type, event_json, history_json, timestamp)
        VALUES (?, ?, ?, ?, ?)
    """

    const val COUNT_ALL_EVENTS = """
        SELECT COUNT(*) FROM event_envelopes
    """

    const val SELECT_EVENTS_FROM_INDEX = """
        SELECT sequence, correlation_id, event_type, event_json, history_json, timestamp
        FROM event_envelopes 
        WHERE sequence > ?
        ORDER BY sequence ASC
    """

    const val SELECT_EVENTS_BY_TYPE_DESC = """
        SELECT sequence, correlation_id, event_type, event_json, history_json, timestamp
        FROM event_envelopes 
        WHERE event_type = ?
        ORDER BY sequence DESC
    """

    // todo - add streaming version and dont use it for regular services
    const val SELECT_ALL_EVENTS_DESC = """
        SELECT sequence, correlation_id, event_type, event_json, history_json, timestamp
        FROM event_envelopes 
        ORDER BY sequence DESC
    """

    // for the stats
    const val SELECT_OLDEST_EVENT = """
        SELECT timestamp
        FROM event_envelopes 
        ORDER BY timestamp ASC
        LIMIT 1
    """

    // for the stats
    const val SELECT_NEWEST_EVENT = """
        SELECT timestamp
        FROM event_envelopes 
        ORDER BY timestamp DESC
        LIMIT 1
    """
}