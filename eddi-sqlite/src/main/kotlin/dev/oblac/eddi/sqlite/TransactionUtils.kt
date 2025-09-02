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

}