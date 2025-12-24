package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventName
import dev.oblac.eddi.Ref
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.db.tables.toEventEnvelope
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun dbFindEventByTag(eventName: EventName, ref: Ref): EventEnvelope<Event>? = transaction {
    addLogger(StdOutSqlLogger)
    DbEvents
        .selectAll()
        .where { DbEvents.name eq eventName.value }
        .andWhere {
            val key = ref.name.value
            val value = ref.seq.value
            val needle = """[{"$key": $value}]"""

            object : Op<Boolean>() {
                override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                    queryBuilder.append(DbEvents.tags)
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


fun dbFindEventByMultipleTags(eventName: EventName, vararg refs: Ref): EventEnvelope<Event>? = transaction {
    addLogger(StdOutSqlLogger)
    DbEvents
        .selectAll()
        .where { DbEvents.name eq eventName.value }
        .apply {
            refs.forEach { ref ->
                andWhere {
                    val key = ref.name.value
                    val value = ref.seq.value
                    val needle = """[{"$key": $value}]"""

                    object : Op<Boolean>() {
                        override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                            queryBuilder.append(DbEvents.tags)
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