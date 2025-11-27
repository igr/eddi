package dev.oblac.eddi.example.college

import dev.oblac.eddi.CommandHandler
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.asyncCommandHandler
import dev.oblac.eddi.example.college.cmd.registerNewStudent
import kotlinx.coroutines.Dispatchers

fun asyncCommands(es: EventStore) =
    asyncCommandHandler(Dispatchers.Default, commandHandler(es))

private fun commandHandler(es: EventStore) = CommandHandler { command ->
    when (command) {
        is RegisterStudent -> registerNewStudent(es, command)
    }
}

