package dev.oblac.eddi.example.college.api

import dev.oblac.eddi.example.college.Main
import dev.oblac.eddi.example.college.PublishCourse
import dev.oblac.eddi.json.Json
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

data class NewCourseRequest(
    val name: String,
    val instructor: String
)

// todo names are wrong
data class NewCourseResponse(
    val uuid: UUID,
)

fun Routing.apiCourses() {
    post("/api/courses") {
        val body = call.receiveText()

        val node = Json.fromJson(body, NewCourseRequest::class)

        val name = node.name
        val instructor = node.instructor

        Main.launch(
            PublishCourse(
                courseName = name,
                instructor = instructor
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
                call.respondText(Json.toJson(StudentResponse(it)), ContentType.Application.Json, HttpStatusCode.Accepted)
            }
        )
    }
}
