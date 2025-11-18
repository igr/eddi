package dev.oblac.eddi


interface EventStoreRepo {

    fun findLastEventByTagBefore(lastEvent: Tag, tag: Tag): EventEnvelope<Event>?
}