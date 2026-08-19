package dev.oblac.eddi

interface EventStoreRepo {

    fun <T: Event> findEventById(eventName: EventName, id: Id): EventEnvelope<T>?

    fun <T: Event> findEventByMultipleIds(eventName: EventName, vararg ids: Id): EventEnvelope<T>?


    fun <T: Event> findEvents(name: EventName, dataFilters: Map<String, String> = mapOf()): List<EventEnvelope<T>>
}

/**
 * Finds the latest [E] carrying [id].
 */
inline fun <reified E : Event> EventStoreRepo.findEventById(id: Id): EventEnvelope<E>? =
    findEventById(EventName.of(E::class), id)

/**
 * Finds the latest [E] carrying all of [ids].
 */
inline fun <reified E : Event> EventStoreRepo.findEventByMultipleIds(vararg ids: Id): EventEnvelope<E>? =
    findEventByMultipleIds(EventName.of(E::class), *ids)

/**
 * Finds all [E] whose payload matches [dataFilters].
 */
inline fun <reified E : Event> EventStoreRepo.findEvents(dataFilters: Map<String, String> = mapOf()): List<EventEnvelope<E>> =
    findEvents(EventName.of(E::class), dataFilters)
