package dev.oblac.eddi.sqlite

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.oblac.eddi.Event
import dev.oblac.eddi.Tag

// Mix-in that tells Jackson to put the concrete class into a property.
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@class"
)
private interface EventMixIn

/**
 * Utility for handling JSON serialization using Jackson in the SQLite Event Store.
 */
object JsonUtils {
    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .addMixIn(Event::class.java, EventMixIn::class.java)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    
    /**
     * Serializes an Event to JSON string using Jackson.
     */
    fun serializeEvent(event: Event): String {
        return objectMapper.writeValueAsString(event)
    }
    
    /**
     * Deserializes an Event from JSON string using Jackson.
     * Uses polymorphic type information stored in JSON to deserialize to correct concrete type.
     */
    fun deserializeEvent(jsonString: String): Event {
        return objectMapper.readValue(jsonString, Event::class.java)
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
    @Suppress("UNCHECKED_CAST")
    fun deserializeHistory(jsonString: String): Map<Tag, Long> {
        return objectMapper.readValue(jsonString, Map::class.java) as Map<Tag, Long>
    }
}
