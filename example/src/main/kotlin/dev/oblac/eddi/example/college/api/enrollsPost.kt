package dev.oblac.eddi.example.college.api

import dev.oblac.eddi.example.college.CoursePublishedTag
import dev.oblac.eddi.example.college.EnrollStudentInCourse
import dev.oblac.eddi.example.college.Main
import dev.oblac.eddi.example.college.StudentRegisteredTag
import dev.oblac.eddi.example.college.projection.dbFindCourseById
import dev.oblac.eddi.example.college.projection.dbFindStudentById
import dev.oblac.eddi.json.Json
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

data class EnrollRequest(
    val course: UUID,
    val student: UUID
)

fun Routing.apiEnrolls() {
    post("/api/enrolls") {
        val body = call.receiveText()
        val node = Json.fromJson(body, EnrollRequest::class)
        val courseId = node.course
        val studentId = node.student

        val student = dbFindStudentById(studentId)!!
        val course = dbFindCourseById(courseId)!!

        Main.launch(
            EnrollStudentInCourse(
                StudentRegisteredTag(student.seq),
                CoursePublishedTag(course.seq),
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
                call.respondText(Json.toJson(StudentResponse(student.id)), ContentType.Application.Json, HttpStatusCode.Accepted)
            }
        )
    }
}
