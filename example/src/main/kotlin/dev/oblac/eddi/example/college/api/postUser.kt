package dev.oblac.eddi.example.college.api

import dev.oblac.eddi.example.college.Main
import dev.oblac.eddi.example.college.RegisterStudent
import dev.oblac.eddi.json.Json
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

data class StudentRequest(
    val firstName: String,
    val lastName: String
)

// todo names are wrong
data class StudentResponse(
    val uuid: UUID,
)

fun Routing.apiStudents() {
    post("/api/students") {
        val body = call.receiveText()

        val node = Json.fromJson(body, StudentRequest::class)

        val firstName = node.firstName
        val lastName = node.lastName

        Main.launch(
            RegisterStudent(
                firstName, lastName, "${firstName.lowercase()}.${lastName.lowercase()}@college.edu"
            )
        ).fold(
            ifLeft = {
                call.respondText(
                    "Error: error",
                    ContentType.Text.Plain,
                    HttpStatusCode.BadRequest
                )
            },
            ifRight = {
                call.respondText(Json.toJson(StudentResponse(it)), ContentType.Text.Plain, HttpStatusCode.Accepted)
            }
        )
    }
}
