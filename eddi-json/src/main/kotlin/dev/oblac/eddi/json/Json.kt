package dev.oblac.eddi.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.oblac.eddi.*
import kotlin.reflect.KClass

private class SeqSerializer : JsonSerializer<Seq>() {
    override fun serialize(value: Seq, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeNumber(value.toLong())
    }
}

private class SeqDeserializer : JsonDeserializer<Seq>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Seq {
        return p.longValue.toSeq()
    }
}

object Json {
    private val eddiModule = SimpleModule("eddi").apply {
        addSerializer(Seq::class.java, SeqSerializer())
        addDeserializer(Seq::class.java, SeqDeserializer())
    }

    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .registerModule(eddiModule)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun <T> toJson(value: T): String =
        objectMapper.writeValueAsString(value)

    /**
     * Deserializes a [JsonNode] into the given class.
     */
    fun <T> fromNode(node: JsonNode, clazz: Class<T>): T =
        objectMapper.treeToValue(node, clazz)

    fun jsonToNode(json: String): JsonNode =
        objectMapper.readTree(json)

    inline fun <reified T> fromJson(json: String): T =
        objectMapper.readValue(json, T::class.java)

    fun <T : Any> fromJson(json: String, klass: KClass<T>): T =
        objectMapper.readValue(json, klass.java)

    fun valueToNode(value: Any): JsonNode =
        objectMapper.valueToTree(value)

    /**
     * Ids as stored with an event: `[{"<id class name>": "<uuid>"}, …]`, keyed by each id's
     * runtime type. Id-based lookups match against this same shape. Deliberately not a Jackson
     * serializer for [Id]: value-class id properties inside an event payload must stay plain uuid strings.
     */
    fun idsToNode(ids: List<Id>): JsonNode =
        valueToNode(ids.map { mapOf(nameOf(it) to it.id) })

    private fun nameOf(id: Id): String =
        id::class.simpleName ?: error("Id class must have a simple name")

}
