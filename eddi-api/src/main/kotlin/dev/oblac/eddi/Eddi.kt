package dev.oblac.eddi

typealias EventListener = (EventEnvelope<Event>) -> Unit

typealias CommandHandler = (Command) -> Unit

