package dev.oblac.eddi.example.college

import dev.oblac.eddi.async
import dev.oblac.eddi.db.Db
import dev.oblac.eddi.db.DbEventStore
import dev.oblac.eddi.db.MigrationConfig
import dev.oblac.eddi.db.tx
import dev.oblac.eddi.example.college.Main.es
import dev.oblac.eddi.meta.EventsRegistry
import dev.oblac.eddi.plus

fun main() {
    // register events before using the event store
    EventsRegistry.init()

    // Database connection with separate schemas for each module
    val db = Db(
        jdbcUrl = "jdbc:postgresql://localhost:7432/eddi",
        username = "eddi_user",
        password = "eddi_password",
        migrations = listOf(
            MigrationConfig(schema = "eddi"),      // Core eddi-db migrations
            MigrationConfig(schema = "college")    // Example college migrations
        )
    )

    startEventsListeners()
    startProjections()
    startWebApp()

    db.close()
}

// Stupid singleton to hold app-wide instances
object Main {
    val es = DbEventStore()

    val launch = commandHandler(es).tx().async()

}

private fun startEventsListeners() {
    val dispatcher = auditEventListener + eventListener
    es.startInbox { dispatcher(it) }
}

private fun startProjections() {
    Projections.start()
}