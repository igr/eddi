package dev.oblac.eddi


interface EventStoreRepo {
    
    fun <T: Event> findLastEventByTagBefore(lastEvent: ULong, tagToFind: TTag<T>): EventEnvelope<T>?
}