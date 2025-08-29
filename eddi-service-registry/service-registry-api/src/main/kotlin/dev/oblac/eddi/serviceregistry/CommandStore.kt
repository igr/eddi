package dev.oblac.eddi.serviceregistry

import dev.oblac.eddi.Command
import dev.oblac.eddi.Event
import dev.oblac.eddi.cmdbus.CommandBus
import dev.oblac.eddi.eventstore.EventStore
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