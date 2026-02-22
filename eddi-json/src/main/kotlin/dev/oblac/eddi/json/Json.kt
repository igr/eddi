package dev.oblac.eddi.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.oblac.eddi.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

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
            seq = field.value.asLong().toSeq()
        )
    }
}

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
        addSerializer(Ref::class.java, RefSerializer())
        addDeserializer(Ref::class.java, RefDeserializer())
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
     * For [Event] classes, uses Java reflection to construct instances directly,
     * bypassing both Jackson's Kotlin module and Kotlin reflection's
     * [ValueClassAwareCaller], which both fail with nested value classes
     * (Tag wraps Seq wraps ULong).
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> fromNode(node: JsonNode, clazz: Class<T>): T {
        if (!Event::class.java.isAssignableFrom(clazz)) {
            return objectMapper.treeToValue(node, clazz)
        }

        val kClass = (clazz as Class<Any>).kotlin
        val kCtor = kClass.primaryConstructor
            ?: return objectMapper.treeToValue(node, clazz)
        val kParams = kCtor.parameters

        // Only use custom deserialization for events that have Tag properties
        val hasTagParams = kParams.any { Tag::class.java.isAssignableFrom(it.type.jvmErasure.java) }
        if (!hasTagParams) {
            return objectMapper.treeToValue(node, clazz)
        }

        val objNode = node as ObjectNode

        // Find the synthetic default-values constructor:
        // (params..., int defaultsMask, DefaultConstructorMarker)
        // Distinguished from the hidden-constructor bridge (params..., DefaultConstructorMarker)
        // by having int as the second-to-last parameter.
        val defaultsCtor = clazz.declaredConstructors.find { ctor ->
            val types = ctor.parameterTypes
            types.size >= 2 &&
                types[types.size - 1] == kotlin.jvm.internal.DefaultConstructorMarker::class.java &&
                types[types.size - 2] == Int::class.javaPrimitiveType
        }

        val jvmCtor = defaultsCtor
            ?: clazz.declaredConstructors.first { it.parameterCount == kParams.size }
        jvmCtor.isAccessible = true

        val jvmParamTypes = jvmCtor.parameterTypes
        val hasDefaults = defaultsCtor != null

        var defaultsMask = 0
        val jvmArgs = mutableListOf<Any?>()

        for ((i, param) in kParams.withIndex()) {
            val paramClass = param.type.jvmErasure
            val jsonValue = objNode.get(param.name!!)
            val jvmType = jvmParamTypes[i]

            if (jsonValue == null || jsonValue.isNull) {
                if (param.isOptional && hasDefaults) {
                    // use default value: set mask bit and pass placeholder
                    defaultsMask = defaultsMask or (1 shl i)
                    jvmArgs.add(jvmDefault(jvmType))
                } else if (param.type.isMarkedNullable) {
                    jvmArgs.add(null)
                } else {
                    error("Missing required non-nullable parameter: ${param.name}")
                }
            } else if (Tag::class.java.isAssignableFrom(paramClass.java)) {
                if (jvmType == Long::class.javaPrimitiveType || jvmType == Long::class.java) {
                    // Non-nullable value class, flattened to long at JVM level
                    jvmArgs.add(jsonValue.asLong())
                } else {
                    // Nullable value class, boxed at JVM level
                    val tagCtor = paramClass.java.declaredConstructors.first { it.parameterCount == 1 }
                    tagCtor.isAccessible = true
                    jvmArgs.add(tagCtor.newInstance(jsonValue.asLong()))
                }
            } else {
                jvmArgs.add(objectMapper.treeToValue(jsonValue, paramClass.java))
            }
        }

        if (hasDefaults) {
            jvmArgs.add(defaultsMask)
            jvmArgs.add(null) // DefaultConstructorMarker
        }

        return jvmCtor.newInstance(*jvmArgs.toTypedArray()) as T
    }

    private fun jvmDefault(type: Class<*>): Any? = when (type) {
        Long::class.javaPrimitiveType -> 0L
        Int::class.javaPrimitiveType -> 0
        Boolean::class.javaPrimitiveType -> false
        Double::class.javaPrimitiveType -> 0.0
        Float::class.javaPrimitiveType -> 0f
        Short::class.javaPrimitiveType -> 0.toShort()
        Byte::class.javaPrimitiveType -> 0.toByte()
        Char::class.javaPrimitiveType -> '\u0000'
        else -> null
    }

    fun jsonToNode(json: String): JsonNode =
        objectMapper.readTree(json)

    inline fun <reified T> fromJson(json: String): T =
        objectMapper.readValue(json, T::class.java)

    fun <T : Any> fromJson(json: String, klass: KClass<T>): T =
        objectMapper.readValue(json, klass.java)

    fun valueToNode(value: Any): JsonNode =
        objectMapper.valueToTree(value)

}
