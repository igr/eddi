package dev.oblac.eddi.example

import dev.oblac.eddi.*

data class SumCommand(val a: Int, val b: Int) : Command
data class PreSumEvent(val result: Int) : Event
data class SumEvent(val result: Int) : Event
data class SumResult(val result: Int)

fun main() {
    val eddi = createMemoryEddie()

    // register services
    eddi.serviceRegistry.registerService(SumCommand::class, ::sum)

    eddi.eventBus.registerEventHandler { eventEnvelope: EventEnvelope<PreSumEvent> ->
        println("Event handled in event bus handler: $eventEnvelope")
        arrayOf(SumEvent(eventEnvelope.event.result + 100))
    }
    eddi.eventBus.registerEventHandler { eventEnvelope: EventEnvelope<SumEvent> ->
        println("Event handled in event bus handler: $eventEnvelope")
        arrayOf()
    }

    // USE #1

    // todo this function is part of the EDDI framework
    fun <C : Command> supplyCommandX(command: C): CommandEnvelope<C> {
        return eddi.commandStore.storeCommand(command)
    }

    supplyCommandX(SumCommand(1, 2))

    // USE #2 - with result handler

    // todo this function is part of the EDDI framework
    fun <C : Command, R> runCommandY(command: C, resultHandler: (R) -> Unit) {
        val cmdEnv = eddi.commandStore.storeCommand(command)
        eddi.projector.registerEphemeralProjectionWaitForCommandResult(cmdEnv.id, resultHandler)
        eddi.projector.projectorForEvent(SumEvent::class, { event ->
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
fun sum(command: SumCommand): Array<PreSumEvent> {
    println("Handling SumCommand: $command")
    return arrayOf(PreSumEvent(command.a + command.b))
}
