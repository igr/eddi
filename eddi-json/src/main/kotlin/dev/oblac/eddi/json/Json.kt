package dev.oblac.eddi.json

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.oblac.eddi.Event
import dev.oblac.eddi.EventName
import dev.oblac.eddi.RefTag
import dev.oblac.eddi.Tag

// Mix-in that tells Jackson to put the concrete class into a property.
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@event"
)
private interface EventMixIn

// Mix-in for Ref to customize field names during serialization
private interface RefMixIn {
    @get:JsonProperty("event")
    val eventName: EventName

    @get:JsonProperty("seq")
    val sequence: ULong
}

// Custom serializer for Tag interface - only serializes the seq field
private class TagSerializer : JsonSerializer<Tag>() {
    override fun serialize(value: Tag, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeNumber(value.seq.toLong())
    }
}

// Custom deserializer for Tag interface - reads only the seq field
private class TagDeserializer : JsonDeserializer<Tag>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Tag {
        val seq = p.longValue.toULong()
        return object : Tag {
            override val name: EventName get() = throw UnsupportedOperationException("Cannot access name during deserialization")
            override val seq: ULong = seq
        }
    }
}

object Json {
    private val tagModule = SimpleModule()
        .addSerializer(Tag::class.java, TagSerializer())
        .addDeserializer(Tag::class.java, TagDeserializer())

    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .registerModule(tagModule)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .addMixIn(Event::class.java, EventMixIn::class.java)
        .addMixIn(RefTag::class.java, RefMixIn::class.java)

    fun <T> toJson(value: T): String =
        objectMapper.writeValueAsString(value)

    inline fun <reified T> fromJson(json: String): T =
        objectMapper.readValue(json, T::class.java)

}
