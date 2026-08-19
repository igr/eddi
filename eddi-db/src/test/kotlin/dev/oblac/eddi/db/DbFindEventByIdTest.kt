package dev.oblac.eddi.db

import dev.oblac.eddi.EventName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DbFindEventByIdTest {

    private val pinged = EventName.of(Pinged::class)
    private val ponged = EventName.of(Ponged::class)

    @BeforeEach
    fun reset() = TestDb.reset()

    @Test
    fun `findEventById matches by id, not just by event name`() {
        val a = PingId(UUID.randomUUID())
        val b = PingId(UUID.randomUUID())
        val storedA = dbStoreEvent(0u, Pinged(a), pinged, listOf(a))
        dbStoreEvent(0u, Pinged(b), pinged, listOf(b))

        assertEquals(storedA.sequence, dbFindEventById(pinged, a)!!.sequence)
        assertNull(dbFindEventById(pinged, PingId(UUID.randomUUID())))
    }

    @Test
    fun `findEventByMultipleIds requires every id`() {
        val a = PingId(UUID.randomUUID())
        val b = PingId(UUID.randomUUID())
        val c = PingId(UUID.randomUUID())
        val storedAb = dbStoreEvent(0u, Ponged(a, b), ponged, listOf(a, b))

        assertEquals(storedAb.sequence, dbFindEventByMultipleIds(ponged, a, b)!!.sequence)
        assertNull(dbFindEventByMultipleIds(ponged, a, c))
    }
}
