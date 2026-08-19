package dev.oblac.eddi

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.util.UUID

@JvmInline
value class FakeId(override val id: UUID) : Id

data class FakeEvent(val fake: FakeId) : Event {
    override fun ids() = listOf(fake)
}

object PingCommand : Command

/**
 * In-memory [EventStore] for session and effect tests: scripted versions, a call log to assert
 * call order, and a switch that makes the conditional append conflict.
 */
class FakeEventStore : EventStore {
    val calls = mutableListOf<String>()
    val versions = mutableMapOf<Id, Seq>()
    var lastExpected: Versions? = null
    var conflictOn: Set<Id>? = null
    private var nextSeq = 0L

    override fun versionOf(id: Id): Seq {
        calls += "versionOf:${id.id}"
        return versions[id] ?: Seq.ZERO
    }

    override fun <T : Event> findEventById(eventName: EventName, id: Id): EventEnvelope<T>? {
        calls += "findEventById:${id.id}"
        return null
    }

    override fun <T : Event> findEventByMultipleIds(eventName: EventName, vararg ids: Id): EventEnvelope<T>? {
        calls += "findEventByMultipleIds:${ids.joinToString(",") { it.id.toString() }}"
        return null
    }

    override fun <E : Event> storeEvent(event: E, correlationId: ULong): EventEnvelope<E> {
        calls += "storeEvent"
        return envelope(event, correlationId)
    }

    override fun <E : Event> storeEvent(
        event: E,
        expected: Versions,
        correlationId: ULong
    ): Either<ConcurrencyConflict, EventEnvelope<E>> {
        calls += "storeEvent:expected"
        lastExpected = expected
        conflictOn?.let { return ConcurrencyConflict(it).left() }
        return envelope(event, correlationId).right()
    }

    private fun <E : Event> envelope(event: E, correlationId: ULong) =
        EventEnvelope(Seq.of(++nextSeq), correlationId, event, EventName.of(event::class))
}
