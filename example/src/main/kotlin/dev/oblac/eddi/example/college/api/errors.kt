package dev.oblac.eddi.example.college.api

import dev.oblac.eddi.json.Json
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.util.UUID

/**
 * JSON error body: `{"error": "<ErrorType>", "message": "<human readable>"}`.
 * The UI's form helper shows `message` when a response is not OK.
 */
data class ErrorResponse(val error: String, val message: String)

suspend fun ApplicationCall.respondError(status: HttpStatusCode, error: String, message: String) =
    respondError(status, ErrorResponse(error, message))

suspend fun ApplicationCall.respondError(status: HttpStatusCode, body: ErrorResponse) =
    respondText(Json.toJson(body), ContentType.Application.Json, status)

/**
 * The request body parsed as [T], or null after answering 400 when it is missing, malformed or
 * lacks a required field.
 */
suspend inline fun <reified T : Any> ApplicationCall.receiveJsonOr400(): T? =
    try {
        Json.fromJson(receiveText(), T::class)
    } catch (e: Exception) {
        respondError(HttpStatusCode.BadRequest, "InvalidRequest", "Malformed request body: ${e.message?.lineSequence()?.firstOrNull()}")
        null
    }

/**
 * The path parameter [name] as a UUID, or null after answering 400 when it is not one.
 */
suspend fun ApplicationCall.uuidParameterOr400(name: String): UUID? {
    val raw = parameters[name]
    return try {
        UUID.fromString(raw)
    } catch (e: Exception) {
        respondError(HttpStatusCode.BadRequest, "InvalidRequest", "'$raw' is not a valid $name")
        null
    }
}
