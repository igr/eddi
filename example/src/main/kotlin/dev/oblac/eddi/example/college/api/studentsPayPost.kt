package dev.oblac.eddi.example.college.api

import dev.oblac.eddi.example.college.Main
import dev.oblac.eddi.example.college.PayTuition
import dev.oblac.eddi.example.college.StudentId
import dev.oblac.eddi.example.college.projection.dbFindStudentById
import dev.oblac.eddi.json.Json
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Routing.apiStudentPay() {
    post("/api/students/{studentId}/pay") {
        val studentId = call.parameters["studentId"]!!
        val student = dbFindStudentById(UUID.fromString(studentId))!!

        Main.launch(
            PayTuition(
                StudentId(student.id)
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
