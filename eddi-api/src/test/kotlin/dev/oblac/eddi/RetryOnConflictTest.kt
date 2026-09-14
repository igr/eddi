package dev.oblac.eddi

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class RetryOnConflictTest {

    private val conflict = ConcurrencyConflict(setOf(FakeId(UUID.randomUUID())))

    /** A handler that returns [results] in order and counts its invocations. */
    private fun scripted(vararg results: Either<CommandError, String>): Pair<CommandHandler<String>, () -> Int> {
        var calls = 0
        val handler = CommandHandler<String> { _ -> results[calls++] }
        return handler to { calls }
    }

    @Test
    fun `re-runs on conflict and returns the first non-conflict result`() {
        val (handler, calls) = scripted(conflict.left(), conflict.left(), "ok".right())

        assertEquals("ok", handler.retryOnConflict(times = 3)(PingCommand).getOrNull())
        assertEquals(3, calls())
    }

    @Test
    fun `gives up after times re-runs and returns the conflict`() {
        val (handler, calls) = scripted(conflict.left(), conflict.left(), "ok".right())

        assertEquals(conflict, handler.retryOnConflict(times = 1)(PingCommand).leftOrNull())
        assertEquals(2, calls())
    }

    @Test
    fun `other errors pass through without re-running`() {
        val (handler, calls) = scripted(UnknownCommandError(PingCommand).left(), "ok".right())

        assertEquals(UnknownCommandError(PingCommand), handler.retryOnConflict()(PingCommand).leftOrNull())
        assertEquals(1, calls())
    }

    @Test
    fun `success passes through without re-running`() {
        val (handler, calls) = scripted("ok".right(), "again".right())

        assertEquals("ok", handler.retryOnConflict()(PingCommand).getOrNull())
        assertEquals(1, calls())
    }
}
