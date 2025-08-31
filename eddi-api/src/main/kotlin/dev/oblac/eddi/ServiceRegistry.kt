package dev.oblac.eddi

import kotlin.reflect.KClass

fun interface Service<C : Command, E : Event> {
    operator fun invoke(command: C): Array<E>
}

interface ServiceRegistry {

    fun <C : Command, E : Event> registerService(command: KClass<C>, service: Service<C, E>)

    fun <C : Command> withService(
        command: KClass<out C>,
        block: (Service<C, Event>) -> Array<Event>): Array<Event>

    fun start(commandBus: CommandBus, eventStore: EventStore)
}

/**
 * Registers a service for the specified command type [C].
 * The service is a function that takes a command of type [C] and returns an array
 * of events of type [E].
 */
inline fun <reified C : Command, E : Event> ServiceRegistry.registerService(
    service: Service<C, E>
) {
    registerService(C::class, service)
}

inline fun <reified C : Command> ServiceRegistry.withService(
    noinline block: (Service<C, Event>) -> Array<Event>
): Array<Event> {
    return withService(C::class, block)
}