package dev.oblac.eddi.example.college

import dev.oblac.eddi.EventListener
import dev.oblac.eddi.on

val auditEventListener = EventListener {
    println("📝 [Audit] ${it.sequence} ${it.eventName}")
}

val eventListener = EventListener { ee ->
    ee.on<StudentRegistered> {
        // none for now
    }
}