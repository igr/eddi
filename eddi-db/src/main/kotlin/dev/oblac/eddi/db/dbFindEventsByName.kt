package dev.oblac.eddi.db

import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.db.tables.toEventEnvelope
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun dbFindEventsByName(name: String, filters: Map<String, String> = mapOf()) = transaction {
    addLogger(StdOutSqlLogger)

    var query = DbEvents
        .selectAll()
        .where { DbEvents.name eq name }

    filters.forEach { (key, value) ->
        query = query.andWhere {
            val jsonExtract = CustomFunction<String?>(
                functionName = "jsonb_extract_path_text",
                columnType = TextColumnType(),
                DbEvents.data,
                stringLiteral(key)
            )
            jsonExtract eq value
        }
    }

    query.map { it.toEventEnvelope() }
}