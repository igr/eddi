package dev.oblac.eddi.example

import dev.oblac.eddi.*
import dev.oblac.eddi.memory.*
import dev.oblac.eddi.sqlite.SqliteEddiFactory


fun createMemoryEddie(): Eddi {
    val commandBus: CommandBus = MemoryCommandBus().also { it.start() }
    // todo dont provide commandBus as ctor arg, but use start() method to pass it
    val commandStore: CommandStore = MemoryCommandStore(commandBus).also { it.start() }
    val eventStoreRepo = MemoryEventStoreRepo()
    val eventStore: EventStore = MemoryEventStore(eventStoreRepo)
    val eventBus: EventBus = MemoryEventBus(eventStore).also { it.start() }
    val eventStoreOutbox: EventStoreOutbox = MemoryEventStoreOutbox(eventBus, eventStoreRepo).also { it.start() }
    val serviceRegistry: ServiceRegistry = MemoryServiceRegistry().also { it.start(commandBus, eventStore) }
    val projector: Projector = MemoryProjector(eventBus)

    return Eddi(
        commandBus,
        commandStore,
        eventBus,
        eventStore,
        eventStoreRepo,
        eventStoreOutbox,
        serviceRegistry,
        projector
    )
}

fun createMemoryPlusSqlite() : Eddi {
    val (eventStore, eventStoreRepo) = SqliteEddiFactory.createEventStore(
        databasePath = "event_store.db"
    )
    val commandBus: CommandBus = MemoryCommandBus().also { it.start() }
    // todo dont provide commandBus as ctor arg, but use start() method to pass it
    val commandStore: CommandStore = MemoryCommandStore(commandBus).also { it.start() }
//    val evetStoreRepo = MemoryEventStoreRepo()
//    val evetStore: EventStore = MemoryEventStore(evetStoreRepo)
    val eventBus: EventBus = MemoryEventBus(eventStore).also { it.start() }
    val eventStoreOutbox: EventStoreOutbox = MemoryEventStoreOutbox(eventBus, eventStoreRepo).also { it.start() }
    val serviceRegistry: ServiceRegistry = MemoryServiceRegistry().also { it.start(commandBus, eventStore) }
    val projector: Projector = MemoryProjector(eventBus)
    return Eddi(
        commandBus,
        commandStore,
        eventBus,
        eventStore,
        eventStoreRepo,
        eventStoreOutbox,
        serviceRegistry,
        projector
    )
}