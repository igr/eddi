package dev.oblac.eddi.memory

import dev.oblac.eddi.*
import kotlin.reflect.KClass

class MemoryServiceRegistry : ServiceRegistry {

    private val serviceRegistry = mutableMapOf<KClass<*>, Service<*, *>>()

    override fun start(commandBus: CommandBus, eventStore: EventStore) {
        commandBus.registerCommandHandler { commandEnvelope ->
            val command = commandEnvelope.command
            @Suppress("UNCHECKED_CAST")
            withService(command::class as KClass<out Command>) { service ->
                service(command)
            }.also { eventStore.storeEvents(commandEnvelope.id, it) }
        }
    }

    override fun <C : Command, E : Event> registerService(
        command: KClass<C>,
        service: Service<C, E>
    ) {
        serviceRegistry[command] = service
    }

    override fun <C : Command> withService(
        command: KClass<out C>,
        block: (Service<C, Event>) -> Array<Event>
    ): Array<Event> {
        return serviceRegistry[command]?.let { service ->
            block(service as Service<C, Event>)
        } ?: emptyArray()
    }
}