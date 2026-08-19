package dev.oblac.eddi.db

import dev.oblac.eddi.ConcurrencyConflict
import dev.oblac.eddi.Seq
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DbEventStoreTest {

    private val store = DbEventStore()

    @BeforeEach
    fun reset() = TestDb.reset()

    @Test
    fun `versionOf reflects stored events`() {
        val id = PingId(UUID.randomUUID())
        assertEquals(Seq.ZERO, store.versionOf(id))

        val stored = store.storeEvent(Pinged(id))

        assertEquals(stored.sequence, store.versionOf(id))
    }

    @Test
    fun `conditional storeEvent appends on current versions and conflicts on stale ones`() {
        val id = PingId(UUID.randomUUID())

        val first = store.storeEvent(Pinged(id), expected = mapOf(id to Seq.ZERO))
        val stale = store.storeEvent(Pinged(id), expected = mapOf(id to Seq.ZERO))

        assertTrue(first.isRight(), first.toString())
        assertEquals(ConcurrencyConflict(setOf(id)), stale.leftOrNull())
        assertEquals(first.getOrNull()!!.sequence, store.versionOf(id))
    }
}
