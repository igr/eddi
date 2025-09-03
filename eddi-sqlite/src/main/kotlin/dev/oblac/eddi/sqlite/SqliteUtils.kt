package dev.oblac.eddi.sqlite

object Sqlite {
    /**
     * Configures SQLite database for optimal performance and concurrency.
     */
    fun configureSqliteForPerformance(database: JdbcDatabase) {
        val commands = listOf(
            //"PRAGMA journal_mode=WAL",
            "PRAGMA synchronous=NORMAL",
            "PRAGMA cache_size=10000",
            "PRAGMA foreign_keys=OFF",
            "PRAGMA temp_store=MEMORY",
            "PRAGMA optimize",
            "PRAGMA busy_timeout=30000", // 30 seconds
            "PRAGMA mmap_size=268435456" // 256MB
        )
        database.executeInBatch(commands)
    }

    /**
     * Performs database maintenance operations to keep SQLite running optimally.
     */
    fun maintainDatabase(database: JdbcDatabase) {
        val maintenanceCommands = listOf(
            "ANALYZE",
            "PRAGMA optimize"
            // "VACUUM" // Commented out as it can be slow on large databases
        )
        database.executeInBatch(maintenanceCommands)
    }
}