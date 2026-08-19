package dev.oblac.eddi.db

import dev.oblac.eddi.ConcurrencyConflict
import dev.oblac.eddi.EventName
import dev.oblac.eddi.Seq
import dev.oblac.eddi.Versions
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread

class DbStoreEventIfTest {

    private val pinged = EventName.of(Pinged::class)

    @BeforeEach
    fun reset() = TestDb.reset()

    @Test
    fun `append succeeds when the expected versions are current`() {
        val id = PingId(UUID.randomUUID())

        val result = dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), mapOf(id to Seq.ZERO))

        assertTrue(result.isRight(), result.toString())
        assertEquals(result.getOrNull()!!.sequence, dbVersionOf(id))
    }

    @Test
    fun `append succeeds on a matching non-zero version`() {
        val id = PingId(UUID.randomUUID())
        dbStoreEvent(0u, Pinged(id), pinged, listOf(id))

        val result = dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), mapOf(id to dbVersionOf(id)))

        assertTrue(result.isRight(), result.toString())
    }

    @Test
    fun `append fails naming the stale id and stores nothing`() {
        val id = PingId(UUID.randomUUID())
        val first = dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), mapOf(id to Seq.ZERO)).getOrNull()!!

        val second = dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), mapOf(id to Seq.ZERO))

        assertEquals(ConcurrencyConflict(setOf(id)), second.leftOrNull())
        assertEquals(first.sequence, dbVersionOf(id))
    }

    @Test
    fun `append rechecks every expected id, not only the ids the event carries`() {
        val read = PingId(UUID.randomUUID())
        val emitted = PingId(UUID.randomUUID())
        dbStoreEvent(0u, Pinged(read), pinged, listOf(read))   // `read` changed after it was "read" at ZERO

        val result = dbStoreEventIf(0u, Pinged(emitted), pinged, listOf(emitted), mapOf(read to Seq.ZERO))

        assertEquals(ConcurrencyConflict(setOf(read)), result.leftOrNull())
        assertEquals(Seq.ZERO, dbVersionOf(emitted))
    }

    @Test
    fun `two appends racing from the same version - exactly one succeeds`() {
        val id = PingId(UUID.randomUUID())
        val expected: Versions = mapOf(id to Seq.ZERO)
        val gate = CyclicBarrier(2)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val results = (1..2).map {
                pool.submit(Callable {
                    gate.await()
                    dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), expected)
                })
            }.map { it.get(30, TimeUnit.SECONDS) }

            assertEquals(1, results.count { it.isRight() }, results.toString())
            assertEquals(ConcurrencyConflict(setOf(id)), results.single { it.isLeft() }.leftOrNull())
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `conditional append refuses to run inside an outer transaction`() {
        val id = PingId(UUID.randomUUID())

        val error = assertThrows<IllegalStateException> {
            transaction {
                dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), mapOf(id to Seq.ZERO))
            }
        }

        assertTrue(error.message!!.contains("outer transaction"), error.message)
        assertEquals(Seq.ZERO, dbVersionOf(id))
    }

    @Test
    fun `unconditional append still returns the stored envelope`() {
        val id = PingId(UUID.randomUUID())

        val stored = dbStoreEvent(7u, Pinged(id), pinged, listOf(id))

        assertEquals(pinged, stored.eventName)
        assertEquals(7uL, stored.correlationId)
        assertEquals(stored.sequence, dbVersionOf(id))
    }

    @Test
    fun `unconditional append refuses to run inside an outer transaction`() {
        val id = PingId(UUID.randomUUID())

        val error = assertThrows<IllegalStateException> {
            transaction {
                dbStoreEvent(0u, Pinged(id), pinged, listOf(id))
            }
        }

        assertTrue(error.message!!.contains("outer transaction"), error.message)
        assertEquals(Seq.ZERO, dbVersionOf(id))
    }

    @Test
    fun `unconditional append waits for the global append lock`() {
        val id = PingId(UUID.randomUUID())

        val stored = whileGlobalAppendLockIsHeld {
            dbStoreEvent(0u, Pinged(id), pinged, listOf(id))
        }

        assertEquals(stored.sequence, dbVersionOf(id))
    }

    @Test
    fun `conditional append waits for the global append lock`() {
        val id = PingId(UUID.randomUUID())

        val result = whileGlobalAppendLockIsHeld {
            dbStoreEventIf(0u, Pinged(id), pinged, listOf(id), mapOf(id to Seq.ZERO))
        }

        assertTrue(result.isRight(), result.toString())
        assertEquals(result.getOrNull()!!.sequence, dbVersionOf(id))
    }

    /**
     * Holds the global append lock from a second connection, runs [append] on another thread,
     * asserts it is still blocked after 500 ms, releases the lock and returns the append's result.
     */
    private fun <T> whileGlobalAppendLockIsHeld(append: () -> T): T {
        val taken = CountDownLatch(1)
        val release = CountDownLatch(1)
        val holder = thread(name = "append-lock-holder") {
            transaction {
                exec("SELECT pg_advisory_xact_lock(hashtextextended('eddi:append', 0))") { }
                taken.countDown()
                release.await(30, TimeUnit.SECONDS)
            }
        }
        assertTrue(taken.await(30, TimeUnit.SECONDS), "could not take the global append lock")
        val pool = Executors.newSingleThreadExecutor()
        try {
            val pending = pool.submit(Callable(append))
            assertThrows<TimeoutException>("append completed although the global append lock was held") {
                pending.get(500, TimeUnit.MILLISECONDS)
            }
            release.countDown()
            return pending.get(30, TimeUnit.SECONDS)
        } finally {
            release.countDown()
            holder.join(30_000)
            pool.shutdownNow()
        }
    }
}
