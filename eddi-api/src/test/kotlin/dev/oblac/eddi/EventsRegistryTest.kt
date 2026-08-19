package dev.oblac.eddi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

private data class Registered(val label: String) : Event {
    override fun ids() = emptyList<Id>()
}

private data class Unregistered(val label: String) : Event {
    override fun ids() = emptyList<Id>()
}

class EventsRegistryTest {

    init {
        Events.register(Registered::class)
    }

    @Test
    fun `nameOf is the event's simple class name`() {
        assertEquals(EventName("Registered"), Events.nameOf(Registered("x")))
    }

    @Test
    fun `nameOf fails for an event class that was not registered`() {
        assertThrows(IllegalStateException::class.java) { Events.nameOf(Unregistered("x")) }
    }

    @Test
    fun `classOf resolves a registered name back to its class`() {
        assertEquals(Registered::class, Events.classOf(EventName("Registered")))
    }
}
