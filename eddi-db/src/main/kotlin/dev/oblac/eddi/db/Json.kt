package dev.oblac.eddi.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object Json {
    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())

    fun <T> toJson(value: T): String = objectMapper.writeValueAsString(value)

    inline fun <reified T> fromJson(json: String): T = objectMapper.readValue(json, T::class.java)
}