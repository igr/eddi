package dev.oblac.eddi

typealias EventListener = (EventEnvelope<Event>) -> Unit
typealias CommandHandler = (Command) -> Unit

fun runCommand(commandHandler: CommandHandler): CommandHandler = { command ->
    runCommand(commandHandler, command)
}

fun runCommand(commandHandler: CommandHandler, command: Command) {
    commandHandler(command)
}


fun dispatchEvent(eventListener: EventListener): EventListener = { envelope ->
    dispatchEvent(eventListener, envelope)
}

fun dispatchEvent(eventListener: EventListener, envelope: EventEnvelope<Event>) {
    eventListener(envelope)
}


operator fun EventListener.plus(
    listener: EventListener
): EventListener = { envelope ->
    this(envelope)
    listener(envelope)
}