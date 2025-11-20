package dev.oblac.eddi


interface EventStoreRepo {

    fun <T: Event> findLastEventByTagBefore(lastEvent: ULong, tagToFind: Tag<T>): EventEnvelope<T>?
}