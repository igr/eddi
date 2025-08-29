package dev.oblac.eddi.serviceregistry

import dev.oblac.eddi.Command
import dev.oblac.eddi.Event
import dev.oblac.eddi.cmdbus.CommandBus
import dev.oblac.eddi.eventstore.EventStore
import kotlin.reflect.KClass

class ServiceRegistryMemory : ServiceRegistry {

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