package dev.oblac.eddi

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right

/**
 * Processes a command by applying a series of validators and then generating an event if all validations pass.
 */
@DslMarker
annotation class CommandDsl

@CommandDsl
class CommandScope<C : Command, E: Event>(val command: C) {
    internal val processors = mutableListOf<CommandProcessor<C>>()
    internal var eventMapper: ((C) -> E)? = null

    operator fun (CommandProcessor<C>).unaryPlus() {
        processors += this
    }

    fun emit(mapper: C.() -> E) {
        eventMapper = { it.mapper() }
    }
}

fun <C : Command, E : Event> process(
    command: C,
    block: CommandScope<C, E>.() -> Unit
): Either<CommandError, E> {
    val scope = CommandScope<C, E>(command).apply(block)
    return scope.processors
        .fold(command.right() as Either<CommandError, C>) { acc, v -> acc.flatMap { v(it) } }
        .map {
            scope.eventMapper!!(it)
        }
}

