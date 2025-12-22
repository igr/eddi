package dev.oblac.eddi.example.college.api

import dev.oblac.eddi.json.Json
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.util.*

data class EnrollRequest(
    val courseId: UUID,
    val studentId: UUID
)

fun Routing.apiEnrolls() {
    post("/api/enrolls") {
        val body = call.receiveText()
        val node = Json.fromJson(body, EnrollRequest::class)
    }
}
