package dev.oblac.eddi

import arrow.core.Either
import arrow.core.flatMap

/**
 * A per-command view of an [EventStore]. Records the version of every id the command reads —
 * before reading it, first touch wins — and appends the resulting event only if none of those
 * versions changed in the meantime.
 *
 * One session per command invocation; a session appends at most once.
 */
class EventStoreSession(private val store: EventStore) : EventStoreRepo {

    private val versions = LinkedHashMap<Id, Seq>()
    private var appended = false

    /** Versions recorded so far, in first-touch order. */
    val recorded: Versions
        get() = versions.toMap()

    override fun <T : Event> findEventById(eventName: EventName, id: Id): EventEnvelope<T>? {
        touch(id)
        return store.findEventById(eventName, id)
    }

    override fun <T : Event> findEventByMultipleIds(eventName: EventName, vararg ids: Id): EventEnvelope<T>? {
        ids.forEach { touch(it) }
        return store.findEventByMultipleIds(eventName, *ids)
    }

    /**
     * Appends [event] if every id read through this session still has the version it had when read.
     */
    fun <E : Event> append(event: E, correlationId: ULong = 0u): Either<ConcurrencyConflict, EventEnvelope<E>> {
        check(!appended) { "EventStoreSession already appended; create a new session per command" }
        appended = true
        return store.storeEvent(event, expected = recorded, correlationId = correlationId)
    }

    /** Records the current version of [id] on first touch — always before the read that follows. */
    private fun touch(id: Id) {
        versions.getOrPut(id) { store.versionOf(id) }
    }
}

/**
 * Opens a session: one per command invocation.
 */
fun EventStore.session(): EventStoreSession = EventStoreSession(this)

/**
 * A [CommandHandler] that runs [decide] with a fresh session per command and appends the emitted
 * event through that same session, so the append is conditional on everything [decide] read.
 */
fun <E : Event> EventStore.commandHandler(
    decide: (EventStoreRepo, Command) -> Either<CommandError, E>
): CommandHandler<EventEnvelope<E>> = CommandHandler<EventEnvelope<E>> { command ->
    val session = session()
    decide(session, command).flatMap { session.append(it) }
}
