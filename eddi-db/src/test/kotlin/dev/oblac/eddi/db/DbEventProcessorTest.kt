package dev.oblac.eddi.db

import dev.oblac.eddi.EventListener
import dev.oblac.eddi.EventName
import dev.oblac.eddi.Seq
import dev.oblac.eddi.db.tables.DbEventsOffsets
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class DbEventProcessorTest {

    private val pinged = EventName.of(Pinged::class)
    private val processors = mutableListOf<DbEventProcessor>()

    @BeforeEach
    fun reset() = TestDb.reset()

    @AfterEach
    fun stopProcessors() = processors.forEach { it.stop() }

    private fun processor(id: Long) = DbEventProcessor(
        processorId = id,
        pollInterval = 10.milliseconds,
        initialBackoff = 10.milliseconds,
        maxBackoff = 50.milliseconds,
    ).also { processors += it }

    private fun store() = dbStoreEvent(0u, Pinged(PingId(UUID.randomUUID())), pinged, listOf())

    private fun lastSeq(processorId: Long): ULong? = transaction {
        DbEventsOffsets
            .select(DbEventsOffsets.lastSequence)
            .where { DbEventsOffsets.id eq processorId }
            .singleOrNull()
            ?.get(DbEventsOffsets.lastSequence)
    }

    private fun await(message: String, timeoutMillis: Long = 10_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) fail<Unit>("timed out waiting for: $message")
            Thread.sleep(20)
        }
    }

    @Test
    fun `delivers events in seq order and advances the offset`() {
        val delivered = CopyOnWriteArrayList<Seq>()
        val first = store()
        val second = store()

        processor(1).startInbox { delivered += it.sequence }
        val third = store()   // stored after the processor started

        await("three deliveries") { delivered.size == 3 }
        assertEquals(listOf(first.sequence, second.sequence, third.sequence), delivered.toList())
        assertEquals(third.sequence.value, lastSeq(1))
    }

    @Test
    fun `a failing delivery is rolled back and retried until it succeeds`() {
        val stored = store()
        val attempts = AtomicInteger()

        processor(2).startInbox { envelope ->
            val attempt = attempts.incrementAndGet()
            transaction {   // joins the processor's transaction
                exec("INSERT INTO test_marks (seq, attempt) VALUES (${envelope.sequence.value}, $attempt)")
            }
            if (attempt == 1) error("boom")
        }

        await("delivery succeeded on retry") { lastSeq(2) == stored.sequence.value }
        val marks = transaction {
            exec("SELECT attempt FROM test_marks ORDER BY attempt") { rs ->
                generateSequence { if (rs.next()) rs.getInt(1) else null }.toList()
            }
        }
        assertEquals(listOf(2), marks)   // the first attempt's row was rolled back with the failure
        assertEquals(2, attempts.get())
    }

    @Test
    fun `two processors with the same id never double-deliver or reorder`() {
        val stored = (1..20).map { store().sequence }
        val delivered = CopyOnWriteArrayList<Seq>()
        val listener = EventListener { delivered += it.sequence; Thread.sleep(5) }

        processor(3).startInbox(listener)
        processor(3).startInbox(listener)

        await("twenty deliveries") { delivered.size >= 20 }
        Thread.sleep(200)   // time for a would-be duplicate to show up
        assertEquals(stored, delivered.toList())
    }

    @Test
    fun `stop ends delivery`() {
        val delivered = CopyOnWriteArrayList<Seq>()
        val processor = processor(4)
        processor.startInbox { delivered += it.sequence }
        store()
        await("first delivery") { delivered.size == 1 }

        processor.stop()
        store()
        Thread.sleep(300)

        assertEquals(1, delivered.size)
    }
}
