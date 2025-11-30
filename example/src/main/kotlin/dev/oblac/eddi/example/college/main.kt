package dev.oblac.eddi.example.college

import dev.oblac.eddi.async
import dev.oblac.eddi.db.Db
import dev.oblac.eddi.db.DbEventStore
import dev.oblac.eddi.db.tx
import dev.oblac.eddi.meta.EventsRegistry

fun main() {
    // register events before using the event store
    EventsRegistry.init()

    // Database connection
    val db = Db(
        jdbcUrl = "jdbc:postgresql://localhost:7432/eddi",
        username = "eddi_user",
        password = "eddi_password"
    )

    Main.hello()

    startWebApp()

    db.close()
}

// Stupid singleton to hold app-wide instances
object Main {
    val es = DbEventStore()

    val launch = commandHandler(es).tx().async()

    fun hello() {
        println("Hello, College Example App!")
    }
}