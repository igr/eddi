package dev.oblac.eddi.example.college.api

import dev.oblac.eddi.example.college.CoursePublished
import dev.oblac.eddi.example.college.PublishCourse
import io.ktor.server.routing.*

data class NewCourseRequest(
    val name: String,
    val instructor: String
)

fun Routing.apiCourses() {
    post("/api/courses") {
        val request = call.receiveJsonOr400<NewCourseRequest>() ?: return@post

        val result = execute(
            PublishCourse(
                courseName = request.name,
                instructor = request.instructor
            )
        )
        call.respondCommand<CoursePublished>(result, created = true) { it.courseId.id }
    }
}
