package dev.oblac.eddi.sqlite

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object Sqlite {
    /**
     * Configures SQLite database for optimal performance and concurrency.
     */
    fun configureSqliteForPerformance(database: Database) {
        val commands = arrayOf(
            //"PRAGMA journal_mode=WAL",
            "PRAGMA synchronous=NORMAL;",
            "PRAGMA cache_size=10000;",
            "PRAGMA foreign_keys=OFF;",
            "PRAGMA temp_store=MEMORY;",
            "PRAGMA optimize;",
            "PRAGMA busy_timeout=30000;", // 30 seconds
            "PRAGMA mmap_size=268435456;" // 256MB
        )
        database.connector().executeInBatch(commands.toList())
    }

    /**
     * Performs database maintenance operations to keep SQLite running optimally.
     */
    fun maintainDatabase(database: Database) {
        transaction(database) {
            // Analyze tables for query optimization
            exec("ANALYZE")

            // Optimize the database structure
            exec("PRAGMA optimize")

            // Clean up any unused space (use sparingly in production)
            // exec("VACUUM") // Commented out as it can be slow on large databases
        }
    }
}