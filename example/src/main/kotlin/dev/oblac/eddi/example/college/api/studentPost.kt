package dev.oblac.eddi.example.college.api

import dev.oblac.eddi.example.college.StudentId
import dev.oblac.eddi.example.college.StudentUpdated
import dev.oblac.eddi.example.college.UpdateStudent
import io.ktor.server.routing.*

data class StudentUpdateRequest(
    val firstName: String,
    val lastName: String
)

fun Routing.apiStudent() {
    post("/api/students/{studentId}") {
        val studentId = call.uuidParameterOr400("studentId") ?: return@post
        val request = call.receiveJsonOr400<StudentUpdateRequest>() ?: return@post

        val result = execute(
            UpdateStudent(
                StudentId(studentId),
                request.firstName,
                request.lastName
            )
        )
        call.respondCommand<StudentUpdated>(result) { it.student.id }
    }
}
