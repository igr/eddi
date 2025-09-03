package dev.oblac.eddi.sqlite

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager

/**
 * Common database support using JDBC without the Exposed dependency.
 * Provides connection management, transaction handling, and SQL utilities.
 */
class JdbcDatabase(
    private val url: String,
    private val properties: Map<String, String> = emptyMap()
) {

    /**
     * Returns a database connection with proper configuration.
     */
    fun connection(): Connection {
        val connection = DriverManager.getConnection(url, properties.toProperties())
        connection.autoCommit = false // Use manual transaction control
        return connection
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

/**
 * Helper extension to convert Map to Properties
 */
private fun Map<String, String>.toProperties(): java.util.Properties {
    val props = java.util.Properties()
    this.forEach { (key, value) -> props.setProperty(key, value) }
    return props
}
