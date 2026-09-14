package dev.oblac.eddi

import arrow.core.left
import arrow.core.right
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class SessionCommandHandlerTest {

    private val store = FakeEventStore()
    private val id = FakeId(UUID.randomUUID())

    private val reading = store.commandHandler<FakeEvent> { repo, _ ->
        repo.findEventById<FakeEvent>(id)
        FakeEvent(id).right()
    }

    @Test
    fun `decides through a session and appends through the same session`() {
        store.versions[id] = Seq.of(7L)

        val result = reading(PingCommand)

        assertTrue(result.isRight())
        assertEquals(mapOf<Id, Seq>(id to Seq.of(7L)), store.lastExpected)
    }

    @Test
    fun `a decision error is returned and nothing is appended`() {
        val failing = store.commandHandler<FakeEvent> { _, command -> UnknownCommandError(command).left() }

        assertEquals(UnknownCommandError(PingCommand), failing(PingCommand).leftOrNull())
        assertNull(store.lastExpected)
        assertFalse(store.calls.any { it.startsWith("storeEvent") })
    }

    @Test
    fun `an append conflict is returned as the command error`() {
        store.conflictOn = setOf(id)

        assertEquals(ConcurrencyConflict(setOf(id)), reading(PingCommand).leftOrNull())
    }

    @Test
    fun `each invocation gets a fresh session`() {
        reading(PingCommand)
        reading(PingCommand)

        // a reused session would record the version once and throw on the second append
        assertEquals(2, store.calls.count { it == "versionOf:${id.id}" })
        assertEquals(2, store.calls.count { it == "storeEvent:expected" })
    }
}
