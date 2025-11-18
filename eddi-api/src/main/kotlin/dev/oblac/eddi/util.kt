package dev.oblac.eddi

inline fun <reified E : Event> EventEnvelope<*>.onEvent(
    crossinline block: (EventEnvelope<E>) -> Unit
) {
    val e = this.event
    if (e is E) block(this as EventEnvelope<E>)
}