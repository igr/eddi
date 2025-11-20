package dev.oblac.eddi


interface EventStoreRepo {

    fun <T: Event> findLastEventByTagBefore(lastEvent: Tag, tagToFind: TTag<T>): EventEnvelope<T>?
}