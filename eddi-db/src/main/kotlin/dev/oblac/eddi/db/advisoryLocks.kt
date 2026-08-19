package dev.oblac.eddi.db

import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.Transaction

/**
 * Transaction-scoped Postgres advisory locks keyed by a string (hashed with `hashtextextended`).
 * Released when the transaction ends; blocks until acquired.
 */
internal fun Transaction.advisoryXactLock(key: String) {
    exec(
        "SELECT pg_advisory_xact_lock(hashtextextended(?::text, 0::bigint))",
        listOf(TextColumnType() to key)
    ) { }
}

/**
 * Non-blocking variant of [advisoryXactLock]: true if acquired, false if another session holds it.
 */
internal fun Transaction.tryAdvisoryXactLock(key: String): Boolean =
    exec(
        "SELECT pg_try_advisory_xact_lock(hashtextextended(?::text, 0::bigint))",
        listOf(TextColumnType() to key)
    ) { rs -> rs.next(); rs.getBoolean(1) } ?: false
