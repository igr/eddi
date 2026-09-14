package dev.oblac.eddi.example.college.api

import dev.oblac.eddi.example.college.CourseId
import dev.oblac.eddi.example.college.EnrollStudentInCourse
import dev.oblac.eddi.example.college.StudentEnrolledInCourse
import dev.oblac.eddi.example.college.StudentId
import io.ktor.server.routing.*
import java.util.*

data class EnrollRequest(
    val course: UUID,
    val student: UUID
)

fun Routing.apiEnrolls() {
    post("/api/enrolls") {
        val request = call.receiveJsonOr400<EnrollRequest>() ?: return@post

        val result = execute(
            EnrollStudentInCourse(
                StudentId(request.student),
                CourseId(request.course),
            )
        )
        call.respondCommand<StudentEnrolledInCourse>(result) { it.student.id }
    }
}
