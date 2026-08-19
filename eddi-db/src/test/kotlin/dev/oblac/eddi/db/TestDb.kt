package dev.oblac.eddi.db

import dev.oblac.eddi.Events
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * One Postgres container for the whole test JVM (Testcontainers' Ryuk removes it at exit).
 * Touching this object starts the container, connects Exposed through [Db] (which also runs
 * the `eddi` Flyway migration) and registers the test events.
 */
object TestDb {
    private val container = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:18-alpine")).also { it.start() }

    val db: Db = Db(container.jdbcUrl, container.username, container.password)

    init {
        Events.register(Pinged::class, Ponged::class)
        transaction {
            // scratch table for listeners under test to write into (joins the processor's transaction)
            exec("CREATE TABLE IF NOT EXISTS test_marks (seq BIGINT NOT NULL, attempt INT NOT NULL)")
        }
    }

    /** Empties the event log (restarting `seq` at 1), the offsets and the scratch table. */
    fun reset() {
        transaction {
            exec("TRUNCATE eddi.events, eddi.events_offsets RESTART IDENTITY CASCADE")
            exec("TRUNCATE test_marks")
        }
    }
}
