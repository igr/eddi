package dev.oblac.eddi.sqlite

/**
 * Schema management without Exposed.
 */
object SqliteSchema {

    const val CREATE_EVENT_ENVELOPES_TABLE = """
        CREATE TABLE IF NOT EXISTS event_envelopes (
            sequence INTEGER PRIMARY KEY AUTOINCREMENT,
            correlation_id INTEGER NOT NULL,
            event_type VARCHAR(255) NOT NULL,
            event_json TEXT NOT NULL,
            history_json TEXT NOT NULL,
            tags_json TEXT NOT NULL,
            timestamp DATETIME NOT NULL
        )
    """

    const val CREATE_SEQUENCE_INDEX = """
        CREATE INDEX IF NOT EXISTS idx_event_envelopes_sequence 
        ON event_envelopes (sequence)
    """

    const val CREATE_EVENT_TYPE_INDEX = """
        CREATE INDEX IF NOT EXISTS idx_event_envelopes_event_type 
        ON event_envelopes (event_type)
    """

    const val CREATE_TIMESTAMP_INDEX = """
        CREATE INDEX IF NOT EXISTS idx_event_envelopes_timestamp 
        ON event_envelopes (timestamp)
    """

    const val CREATE_EVENT_TYPE_TIMESTAMP_INDEX = """
        CREATE INDEX IF NOT EXISTS idx_event_envelopes_event_type_timestamp 
        ON event_envelopes (event_type, timestamp)
    """

    const val CREATE_CORRELATION_ID_INDEX = """
        CREATE INDEX IF NOT EXISTS idx_event_envelopes_correlation_id 
        ON event_envelopes (correlation_id)
    """

    /**
     * Creates all necessary tables and indexes.
     */
    fun createSchema(database: JdbcDatabase) {
        val schemaCommands = listOf(
            CREATE_EVENT_ENVELOPES_TABLE,
            CREATE_SEQUENCE_INDEX,
            CREATE_EVENT_TYPE_INDEX,
            CREATE_TIMESTAMP_INDEX,
            CREATE_EVENT_TYPE_TIMESTAMP_INDEX,
            CREATE_CORRELATION_ID_INDEX
        )

        database.executeInBatch(schemaCommands)
    }
}