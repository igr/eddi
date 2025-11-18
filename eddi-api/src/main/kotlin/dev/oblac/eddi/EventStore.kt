package dev.oblac.eddi


interface EventStoreRepo {

    fun <E : Event> findLastEventByTagBefore(lastEvent: EventEnvelope<E>, ref: RefTag): EventEnvelope<Event>?
}