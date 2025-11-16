package dev.oblac.eddi.json

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.oblac.eddi.Event
import dev.oblac.eddi.Tag

object Json {
    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Event::class.java)
                .allowIfSubType(Tag::class.java)
                .allowIfBaseType(Collection::class.java)
                .allowIfSubType(Collection::class.java)
                .build(),
            ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
            JsonTypeInfo.As.PROPERTY
        )

    fun <T> toJson(value: T): String =
        objectMapper.writeValueAsString(value)

    fun <T: Event> toEventJson(value: T): String =
        objectMapper.writerFor(Event::class.java).writeValueAsString(value)

    fun <T: Tag> toTagJson(value: T): String =
        objectMapper.writerFor(Tag::class.java).writeValueAsString(value)

    fun <T: Tag> toTagJson(value: Array<T>): String =
        toJson(value)

    inline fun <reified T> fromJson(json: String): T =
        objectMapper.readValue(json, T::class.java)
}
