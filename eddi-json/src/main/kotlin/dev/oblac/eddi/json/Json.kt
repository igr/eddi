package dev.oblac.eddi.json

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.oblac.eddi.Event

// Mix-in that tells Jackson to put the concrete class into a property.
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@event"
)
private interface EventMixIn

object Json {
    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .addMixIn(Event::class.java, EventMixIn::class.java)

    fun <T> toJson(value: T): String =
        objectMapper.writeValueAsString(value)

    inline fun <reified T> fromJson(json: String): T =
        objectMapper.readValue(json, T::class.java)

}
