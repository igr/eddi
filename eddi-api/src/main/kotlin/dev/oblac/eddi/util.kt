package dev.oblac.eddi

/**
 * Executes the given [block] if the event in this [EventEnvelope] is of type [E].
 * Utility function for type-safe event handling.
 */
inline fun <reified E : Event> EventEnvelope<*>.onEvent(
    crossinline block: (EventEnvelope<E>) -> Unit
) {
    val e = this.event
    if (e is E) block(this as EventEnvelope<E>)
}

/**
 * Combines two [EventListener]s into one that executes both in sequence.
 */
operator fun EventListener.plus(
    listener: EventListener
): EventListener = { envelope ->
    this(envelope)
    listener(envelope)
}