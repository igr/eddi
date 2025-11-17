package dev.oblac.eddi


interface EventStoreRepo {

    fun findLastEventByTag(tag: Tag): EventEnvelope<Event>?
}