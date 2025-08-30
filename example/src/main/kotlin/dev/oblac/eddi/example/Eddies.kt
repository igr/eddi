package dev.oblac.eddi.example

import dev.oblac.eddi.*
import dev.oblac.eddi.memory.*


fun createMemoryEddie(): Eddi {
    val commandBus: CommandBus = MemoryCommandBus().also { it.start() }
    // todo dont provide commandBus as ctor arg, but use start() method to pass it
    val commandStore: CommandStore = MemoryCommandStore(commandBus).also { it.start() }
    val eventBus: EventBus = MemoryEventBus().also { it.start() }
    val evetStore: EventStore = MemoryEventStore(eventBus).also { it.start() }
    val serviceRegistry: ServiceRegistry = MemoryServiceRegistry().also { it.start(commandBus, evetStore) }
    val projector: Projector = MemoryProjector(eventBus)

    val eddi = Eddi(commandBus, commandStore, eventBus, evetStore, serviceRegistry, projector)
    return eddi
}