package dev.oblac.eddi.sqlite

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import dev.oblac.eddi.Event
import dev.oblac.eddi.Tag

/**
 * Utility object for handling JSON serialization using Jackson in the SQLite Event Store.
 */
object JsonUtils {
    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION, true)
    
    /**
     * Serializes an Event to JSON string using Jackson.
     */
    fun serializeEvent(event: Event): String {
        return objectMapper.writeValueAsString(event)
    }
    
    /**
     * Deserializes an Event from JSON string using Jackson.
     */
//    fun <T : Event> deserializeEvent(jsonString: String): T {
////        return objectMapper.readValue(jsonString, jacksonTypeRef<T>()) as T
//        return deserializeEventX(objectMapper, jsonString) as T
//    }

    inline fun <reified T : Event> deserializeEvent(jsonString: String): T {
        return objectMapper.readValue(jsonString, jacksonTypeRef<T>())
    }
    
    /**
     * Serializes a Map<Tag, Long> to JSON string using Jackson.
     */
    fun serializeHistory(history: Map<Tag, Long>): String {
        return objectMapper.writeValueAsString(history)
    }
    
    /**
     * Deserializes a Map<Tag, Long> from JSON string using Jackson.
     */
    fun deserializeHistory(jsonString: String): Map<Tag, Long> {
        return objectMapper.readValue(jsonString, Map::class.java) as Map<Tag, Long>
    }
}
