package dev.oblac.eddi

typealias EventListener = (EventEnvelope<Event>) -> Unit
typealias CommandHandler = (Command) -> Sequence<Event>

fun runCommand(
    commandHandler: CommandHandler,
    eventStoreInbox: EventStoreInbox,
): (Command) -> Sequence<EventEnvelope<Event>> = { command ->
    runCommand(commandHandler, eventStoreInbox, command, correlationId = 0L)
}

fun runCommand(
    commandHandler: CommandHandler,
    eventStoreInbox: EventStoreInbox,
    command: Command,
    correlationId: Long = 0L
): Sequence<EventEnvelope<Event>> {
    val events = commandHandler(command).toList()
    println("runCommand: produced ${events.size} events for command: $command")
    return events.map {
        eventStoreInbox.storeEvent(correlationId, it)
    }.asSequence()
}

fun dispatchEvent(
    eventListener: EventListener,
    envelope: EventEnvelope<Event>
) {
    return eventListener(envelope)
}

fun dispatchEvent(
    eventListener: EventListener
): EventListener = { envelope ->
    dispatchEvent(eventListener, envelope)
}

operator fun EventListener.plus(
    listener: EventListener
): EventListener = { envelope ->
    this(envelope)
    listener(envelope)
}