package dev.oblac.eddi.example.college

import dev.oblac.eddi.CommandHandler
import dev.oblac.eddi.EventStoreInbox
import dev.oblac.eddi.asyncCommandHandler
import kotlinx.coroutines.Dispatchers

fun asyncCommands(esInbox: EventStoreInbox) =
    asyncCommandHandler(Dispatchers.Default, commandHandler(esInbox))

private fun commandHandler(esInbox: EventStoreInbox) = CommandHandler { command ->
    when (command) {
        is RegisterStudent -> registerNewStudent(esInbox, command)
    }
}

/**
 * Registers a new student.
 */
private fun registerNewStudent(inbox: EventStoreInbox, command: RegisterStudent) {
    inbox.storeEvent(
        StudentRegistered(
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email
        )
    )
}
