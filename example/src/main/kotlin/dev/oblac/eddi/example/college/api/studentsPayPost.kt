package dev.oblac.eddi.example.college.api

import dev.oblac.eddi.example.college.PayTuition
import dev.oblac.eddi.example.college.StudentId
import dev.oblac.eddi.example.college.TuitionPaid
import io.ktor.server.routing.*

fun Routing.apiStudentPay() {
    post("/api/students/{studentId}/pay") {
        val studentId = call.uuidParameterOr400("studentId") ?: return@post

        val result = execute(PayTuition(StudentId(studentId)))
        call.respondCommand<TuitionPaid>(result) { it.student.id }
    }
}
