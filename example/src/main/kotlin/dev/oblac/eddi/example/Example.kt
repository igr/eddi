package dev.oblac.eddi.example

import dev.oblac.eddi.Command
import dev.oblac.eddi.CommandEnvelope
import dev.oblac.eddi.Event
import dev.oblac.eddi.cmdbus.CommandBus
import dev.oblac.eddi.cmdbus.CommandBusMemory
import dev.oblac.eddi.cmdstore.CommandStore
import dev.oblac.eddi.cmdstore.CommandStoreMemory
import dev.oblac.eddi.eventbus.EventBus
import dev.oblac.eddi.eventbus.EventBusMemory
import dev.oblac.eddi.eventstore.EventStore
import dev.oblac.eddi.eventstore.EventStoreMemory
import dev.oblac.eddi.projector.Projector
import dev.oblac.eddi.projector.ProjectorMemory
import dev.oblac.eddi.serviceregistry.ServiceRegistry
import dev.oblac.eddi.serviceregistry.ServiceRegistryMemory

data class SumCommand(val a: Int, val b: Int) : Command
data class SumEvent(val result: Int) : Event
data class SumResult(val result: Int)


fun main() {
    // wire
    val commandBus: CommandBus = CommandBusMemory().also { it.start() }
    // todo dont provide commandBus as ctor arg, but use start() method to pass it
    val commandStore: CommandStore = CommandStoreMemory(commandBus).also { it.start() }
    val eventBus: EventBus = EventBusMemory().also { it.start() }
    val evetStore: EventStore = EventStoreMemory(eventBus).also { it.start() }
    val serviceRegistry: ServiceRegistry = ServiceRegistryMemory().also { it.start(commandBus, evetStore) }
    val projector: Projector = ProjectorMemory().also { it.start(eventBus) }

    // register services
    serviceRegistry.registerService(SumCommand::class, ::sum)

    // USE #1

    fun <C : Command> supplyCommandX(command: C): CommandEnvelope<C> {
        return commandStore.storeCommand(command)
    }

    supplyCommandX(SumCommand(1, 2))

    // USE #2 - with result handler

    fun <C : Command, R> runCommandY(command: C, resultHandler: (R) -> Unit) {
        val cmdEnv = commandStore.storeCommand(command)
        projector.registerEphemeralProjectionWaitForCommandResult(cmdEnv.id, resultHandler)
        projector.projectorForEvent(SumEvent::class, { event ->
            println("Event handled in main(): $event")
            SumResult(event.result)
        })
    }

    runCommandY(SumCommand(1, 2), { result: SumResult ->
        println("Sum result is: ${result.result}")
    })

    readLine()
}

// service
fun sum(command: SumCommand): Array<Event> {
    println("Handling SumCommand: $command")
    return arrayOf(SumEvent(command.a + command.b))
}
