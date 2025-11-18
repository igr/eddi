package dev.oblac.eddi


interface EventStoreRepo {

    // TODO Return type should be stronger typed
    fun findLastEventByTagBefore(lastEvent: Tag, tagToFind: Tag): EventEnvelope<Event>?
}