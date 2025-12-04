package dev.oblac.eddi.example.college

import dev.oblac.eddi.example.college.api.apiStudent
import dev.oblac.eddi.example.college.api.apiStudentPay
import dev.oblac.eddi.example.college.api.apiStudents
import dev.oblac.eddi.example.college.ui.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

fun startWebApp() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
    }.start(wait = true)
}

fun Application.configureRouting() {
    routing {
        pageIndex()
        pageStudents()
        pageStudentAdd()
        pageStudentEdit()
        pageStudentPay()
        apiStudents()
        apiStudent()
        apiStudentPay()
    }
}