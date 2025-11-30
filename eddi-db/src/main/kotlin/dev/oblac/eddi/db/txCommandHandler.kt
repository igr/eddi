package dev.oblac.eddi.db

import arrow.core.Either
import dev.oblac.eddi.Command
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.CommandHandler
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * A CommandHandler that wraps the target handler execution in a database transaction.
 * The transaction is committed if the handler returns a successful result,
 * and rolled back if an error occurs.
 *
 * @param R the result type of the command handler
 * @param target the wrapped CommandHandler to execute within a transaction
 */
class TxCommandHandler<R>(
    private val target: CommandHandler<R>
) : CommandHandler<R> {
    override fun invoke(command: Command): Either<CommandError, R> {
        return transaction {
            target(command)
        }
    }
}

/**
 * Extension function to wrap a CommandHandler in a transaction.
 * This applies transaction management as an effect to the handler.
 *
 * Example:
 * ```
 * val handler = studentHandler
 *     .asContext()
 *     .map { it.id }
 *     .build()
 *     .tx()  // Wrap in transaction
 * ```
 */
fun <R> CommandHandler<R>.tx(): TxCommandHandler<R> =
    TxCommandHandler(this)
