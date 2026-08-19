package dev.oblac.eddi.db

import dev.oblac.eddi.Id
import dev.oblac.eddi.db.tables.DbEvents
import dev.oblac.eddi.json.Json
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.stringLiteral

/**
 * `ids @> '[{"<IdClass>": "<uuid>"}]'::jsonb` — the event carries [id].
 * Served by the GIN index on `eddi.events.ids`.
 */
internal fun idsContain(id: Id): Op<Boolean> = object : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append(DbEvents.ids)
        queryBuilder.append(" @> ")
        queryBuilder.append(stringLiteral(Json.idsToNode(listOf(id)).toString()))
        queryBuilder.append("::jsonb")
    }
}
