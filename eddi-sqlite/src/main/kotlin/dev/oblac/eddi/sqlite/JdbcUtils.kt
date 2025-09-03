package dev.oblac.eddi.sqlite

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

/**
 * Common database support using JDBC without the Exposed dependency.
 * Provides connection management, transaction handling, and SQL utilities.
 */
class JdbcDatabase(
    private val url: String,
    private val properties: Map<String, String> = emptyMap()
) : AutoCloseable {

    private val dataSource: HikariDataSource

    init {
        val config = HikariConfig().apply {
            jdbcUrl = url
            
            // Apply custom properties
            properties.forEach { (key, value) ->
                dataSourceProperties.setProperty(key, value)
            }
            
            // SQLite-specific optimizations
            maximumPoolSize = 1 // SQLite only supports one writer at a time
            minimumIdle = 1
            maxLifetime = 300_000 // 5 minutes
            idleTimeout = 60_000 // 1 minute
            connectionTimeout = 30_000 // 30 seconds
            leakDetectionThreshold = 60_000 // 1 minute
            
            // Connection pool name for monitoring
            poolName = "SQLite-Pool"
            
            // Auto-commit should be false for transaction control
            isAutoCommit = false
        }
        
        dataSource = HikariDataSource(config)
    }

    /**
     * Returns a database connection from the connection pool.
     */
    fun connection(): Connection = dataSource.connection

    /**
     * Closes the connection pool and releases all resources.
     * Should be called during application shutdown.
     */
    override fun close() {
        if (!dataSource.isClosed) {
            dataSource.close()
        }
    }

    /**
     * Executes a transaction block with proper error handling and retry logic.
     */
    suspend fun <T> transaction(
        maxRetries: Int = 3,
        block: suspend (Connection) -> T
    ): T = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            val connection = connection()
            try {
                val result = block(connection)
                connection.commit()
                return@withContext result
            } catch (e: Exception) {
                connection.rollback()
                lastException = e

                if (!isRetryableError(e) || attempt == maxRetries - 1) {
                    throw SqliteEventStoreException(
                        "Transaction failed after ${attempt + 1} attempts",
                        e
                    )
                }

                // Brief delay before retry (exponential backoff)
                kotlinx.coroutines.delay((1L shl attempt) * 10)
            } finally {
                connection.close()
            }
        }

        throw SqliteEventStoreException(
            "Transaction failed after $maxRetries attempts",
            lastException
        )
    }

    /**
     * Executes multiple SQL commands in batch for better performance.
     */
    fun executeInBatch(commands: List<String>) {
        connection().use { connection ->
            connection.createStatement().use { statement ->
                commands.forEach { command ->
                    statement.addBatch(command)
                }
                statement.executeBatch()
                connection.commit()
            }
        }
    }

    /**
     * Determines if an exception represents a retryable database error.
     */
    private fun isRetryableError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""
        return when {
            // SQLite busy/locked errors
            message.contains("database is locked") -> true
            message.contains("database is busy") -> true
            message.contains("sqlite_busy") -> true
            message.contains("sqlite_locked") -> true

            // Connection issues
            message.contains("connection") && message.contains("closed") -> true

            // Timeout errors
            message.contains("timeout") -> true

            else -> false
        }
    }
}

