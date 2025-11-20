package dev.oblac.eddi.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.oblac.eddi.EventName
import dev.oblac.eddi.Ref
import dev.oblac.eddi.Tag
import kotlin.reflect.KClass

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

private class RefSerializer : JsonSerializer<Ref>() {
    override fun serialize(value: Ref, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNumberField(value.name.value, value.seq.toLong())
        gen.writeEndObject()
    }
}

private class RefDeserializer : JsonDeserializer<Ref>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Ref {
        val node = p.codec.readTree<JsonNode>(p)
        val field = node.fields().next()
        return Ref(
            name = EventName(field.key),
            seq = field.value.asLong().toULong()
        )
    }
}

object Json {
    private val tagModule = SimpleModule()
        .addSerializer(Tag::class.java, TagSerializer())
        .addDeserializer(Tag::class.java, TagDeserializer())
        .addSerializer(Ref::class.java, RefSerializer())
        .addDeserializer(Ref::class.java, RefDeserializer())


    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .registerModule(tagModule)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun <T> toJson(value: T): String =
        objectMapper.writeValueAsString(value)

    fun <T> fromNode(node: JsonNode, clazz: Class<T>): T =
        objectMapper.treeToValue(node, clazz)

    fun toNode(json: String): JsonNode =
        objectMapper.readTree(json)

    inline fun <reified T> fromJson(json: String): T =
        objectMapper.readValue(json, T::class.java)

    fun <T : Any> fromJson(json: String, klass: KClass<T>): T =
        objectMapper.readValue(json, klass.java)

    fun toNode(value: Any): JsonNode =
        objectMapper.valueToTree(value)

}
