package dev.oblac.eddi.sqlite

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object Sqlite {
    /**
     * Configures SQLite database for optimal performance and concurrency.
     */
    fun configureSqliteForPerformance(database: Database) {
        transaction(database) {
            exec("PRAGMA journal_mode=WAL")

            // Balanced synchronous mode (better performance than FULL, safer than OFF)
            exec("PRAGMA synchronous=NORMAL")

            // Larger cache for better performance
            exec("PRAGMA cache_size=10000")

            // Disable foreign key constraints for performance (we manage integrity)
            exec("PRAGMA foreign_keys=OFF")

            // Use memory for temporary storage
            exec("PRAGMA temp_store=MEMORY")

            // Optimize for append-heavy workloads
            exec("PRAGMA optimize")

            // Set a reasonable timeout for busy situations
            exec("PRAGMA busy_timeout=30000") // 30 seconds

            // Enable memory-mapped I/O for better performance on modern systems
            exec("PRAGMA mmap_size=268435456") // 256MB
        }
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