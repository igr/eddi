package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventName
import dev.oblac.eddi.Id
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.db.tables.toEventEnvelope
import dev.oblac.eddi.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun dbFindEventById(eventName: EventName, id: Id): EventEnvelope<Event>? = transaction {
    addLogger(StdOutSqlLogger)
    DbEvents
        .selectAll()
        .where { DbEvents.name eq eventName.value }
        .andWhere {
            val needle = Json.idsToNode(listOf(id)).toString()

            object : Op<Boolean>() {
                override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                    queryBuilder.append(DbEvents.ids)
                    queryBuilder.append(" @> ")
                    queryBuilder.append(stringLiteral(needle))
                    queryBuilder.append("::jsonb")
                }
            }
        }
        .orderBy(DbEvents.sequence, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}


fun dbFindEventByMultipleIds(eventName: EventName, vararg ids: Id): EventEnvelope<Event>? = transaction {
    addLogger(StdOutSqlLogger)
    DbEvents
        .selectAll()
        .where { DbEvents.name eq eventName.value }
        .apply {
            ids.forEach { id ->
                andWhere {
                    val needle = Json.idsToNode(listOf(id)).toString()

                    object : Op<Boolean>() {
                        override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                            queryBuilder.append(DbEvents.ids)
                            queryBuilder.append(" @> ")
                            queryBuilder.append(stringLiteral(needle))
                            queryBuilder.append("::jsonb")
                        }
                    }
                }
            }
        }
        .orderBy(DbEvents.sequence, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
        ?.toEventEnvelope()
}