package dev.oblac.eddi.example

import dev.oblac.eddi.*
import dev.oblac.eddi.memory.*


fun createMemoryEddie(): Eddi {
    val commandBus: CommandBus = MemoryCommandBus().also { it.start() }
    // todo dont provide commandBus as ctor arg, but use start() method to pass it
    val commandStore: CommandStore = MemoryCommandStore(commandBus).also { it.start() }
    val evetStoreRepo = MemoryEventStoreRepo()
    val evetStore: EventStore = MemoryEventStore(evetStoreRepo)
    val eventBus: EventBus = MemoryEventBus(evetStore).also { it.start() }
    val eventStoreOutbox: EventStoreOutbox = MemoryEventStoreOutbox(eventBus, evetStoreRepo).also { it.start() }
    val serviceRegistry: ServiceRegistry = MemoryServiceRegistry().also { it.start(commandBus, evetStore) }
    val projector: Projector = MemoryProjector(eventBus)

    return Eddi(
        commandBus,
        commandStore,
        eventBus,
        evetStore,
        evetStoreRepo,
        eventStoreOutbox,
        serviceRegistry,
        projector
    )
}