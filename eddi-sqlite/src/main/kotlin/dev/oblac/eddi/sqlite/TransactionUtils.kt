package dev.oblac.eddi.sqlite

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Utility functions for handling SQLite transactions with proper error handling
 * and performance optimizations.
 */
object TransactionUtils {
    
    /**
     * Executes a database transaction with retry logic and proper error handling.
     * 
     * @param database The database instance
     * @param maxRetries Maximum number of retry attempts for failed transactions
     * @param block The transaction block to execute
     * @return Result of the transaction block
     * @throws SqliteEventStoreException if all retries fail
     */
    fun <T> safeTransaction(
        database: Database,
        maxRetries: Int = 3,
        block: Transaction.() -> T
    ): T {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return transaction(database) {
                    block()
                }
            } catch (e: Exception) {
                lastException = e
                
                // Check if this is a retryable error
                if (!isRetryableError(e) || attempt == maxRetries - 1) {
                    throw SqliteEventStoreException(
                        "Transaction failed after ${attempt + 1} attempts",
                        e
                    )
                }
                
                // Brief delay before retry (exponential backoff)
                try {
                    Thread.sleep((1L shl attempt) * 10) // 10ms, 20ms, 40ms...
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw SqliteEventStoreException("Transaction interrupted", ie)
                }
            }
        }
        
        throw SqliteEventStoreException(
            "Transaction failed after $maxRetries attempts",
            lastException
        )
    }
    
    /**
     * Executes a suspending database transaction using the IO dispatcher.
     * 
     * @param database The database instance
     * @param maxRetries Maximum number of retry attempts
     * @param block The suspending transaction block to execute
     * @return Result of the transaction block
     */
    suspend fun <T> suspendingTransaction(
        database: Database,
        maxRetries: Int = 3,
        block: suspend Transaction.() -> T
    ): T = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return@withContext transaction(database) {
                    kotlinx.coroutines.runBlocking {
                        block()
                    }
                }
            } catch (e: Exception) {
                lastException = e
                
                if (!isRetryableError(e) || attempt == maxRetries - 1) {
                    throw SqliteEventStoreException(
                        "Suspending transaction failed after ${attempt + 1} attempts",
                        e
                    )
                }
                
                // Suspending delay
                kotlinx.coroutines.delay((1L shl attempt) * 10)
            }
        }
        
        throw SqliteEventStoreException(
            "Suspending transaction failed after $maxRetries attempts",
            lastException
        )
    }
    
    /**
     * Determines if an exception represents a retryable database error.
     */
    internal fun isRetryableError(exception: Exception): Boolean {
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