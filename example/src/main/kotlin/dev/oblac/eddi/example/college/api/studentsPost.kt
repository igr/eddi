package dev.oblac.eddi.example.college.api

import dev.oblac.eddi.example.college.RegisterStudent
import dev.oblac.eddi.example.college.StudentRegistered
import io.ktor.server.routing.*

data class StudentRequest(
    val firstName: String,
    val lastName: String
)

fun Routing.apiStudents() {
    post("/api/students") {
        val request = call.receiveJsonOr400<StudentRequest>() ?: return@post
        val firstName = request.firstName
        val lastName = request.lastName

        val result = execute(
            RegisterStudent(
                firstName, lastName, "${firstName.lowercase()}.${lastName.lowercase()}@college.edu"
            )
        )
        call.respondCommand<StudentRegistered>(result, created = true) { it.studentId.id }
    }
}
