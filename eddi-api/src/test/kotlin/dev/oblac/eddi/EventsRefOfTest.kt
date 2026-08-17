package dev.oblac.eddi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

private data class Registered(val label: String) : Event

@JvmInline
private value class RegisteredId(override val id: UUID) : Tag<Registered>

class EventsRefOfTest {
    @Test
    fun `refOf builds a Ref from the tag's uuid`() {
        val name = EventName("Registered")
        Events.register(RegisteredId::class, name)

        val uuid = UUID.fromString("3f2a0000-0000-0000-0000-000000000001")

        assertEquals(Ref(name, uuid), Events.refOf(RegisteredId(uuid)))
    }
}
