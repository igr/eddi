package dev.oblac.eddi.db

import dev.oblac.eddi.EventName
import dev.oblac.eddi.Seq
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DbVersionOfTest {

    private val pinged = EventName.of(Pinged::class)
    private val ponged = EventName.of(Ponged::class)

    @BeforeEach
    fun reset() = TestDb.reset()

    @Test
    fun `version is ZERO when no event carries the id`() {
        assertEquals(Seq.ZERO, dbVersionOf(PingId(UUID.randomUUID())))
    }

    @Test
    fun `version is the sequence of the latest event carrying the id, including multi-id events`() {
        val a = PingId(UUID.randomUUID())
        val b = PingId(UUID.randomUUID())
        dbStoreEvent(0u, Pinged(a), pinged, listOf(a))
        val second = dbStoreEvent(0u, Ponged(a, b), ponged, listOf(a, b))
        val third = dbStoreEvent(0u, Pinged(b), pinged, listOf(b))

        assertEquals(second.sequence, dbVersionOf(a))
        assertEquals(third.sequence, dbVersionOf(b))
    }
}
