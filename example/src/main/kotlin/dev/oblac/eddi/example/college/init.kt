package dev.oblac.eddi.example.college

import dev.oblac.eddi.db.Db
import dev.oblac.eddi.meta.EventsRegistry

private var db: Db? = null

fun init() {
    // we need to register events before using the event store
    EventsRegistry.init()

    // Database connection
    val db = Db(
        jdbcUrl = "jdbc:postgresql://localhost:7432/eddi",
        username = "eddi_user",
        password = "eddi_password"
    )
}

fun shutdown() {
    db?.close()
}