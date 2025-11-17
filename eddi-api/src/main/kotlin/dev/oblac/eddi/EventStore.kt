package dev.oblac.eddi


interface EventStoreRepo {

    fun <E : Event> findLastEventByTagBefore(lastEvent: EventEnvelope<E>, tag: Tag): EventEnvelope<Event>?
}