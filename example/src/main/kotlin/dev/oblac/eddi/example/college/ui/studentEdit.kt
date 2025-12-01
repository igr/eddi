package dev.oblac.eddi.example.college.ui

import dev.oblac.eddi.example.college.projection.dbFindStudentById
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*
import java.util.*

fun Routing.pageStudentEdit() {
    get("/student-edit.html") {
        val studentId = call.parameters["id"]
        if (studentId == null) {
            call.respondHtml {
                head()
                body {
                    div("container") {
                        h1 { +"Error" }
                        a (href = "/") { +"Home" }
                        p { +"Missing student ID." }
                    }
                }
            }
            return@get
        }
        val student = dbFindStudentById(UUID.fromString(studentId))
        if (student == null) {
            call.respondHtml {
                head()
                body {
                    div("container") {
                        h1 { +"Error" }
                        a (href = "/") { +"Home" }
                        p { +"Student not found." }
                    }
                }
            }
            return@get
        }
        call.respondHtml {
            head()
            body {
                div("container") {
                    h1 { +"Edit Student" }
                    a (href = "/") { +"Home" }
                    form {
                        id = "apiForm"
                        input(type = InputType.text) {
                            name = "firstName"
                            placeholder = "First Name"
                            value = student.firstName
                        }
                        input(type = InputType.text) {
                            name = "lastName"
                            placeholder = "Last Name"
                            value = student.lastName
                        }
                        button(type = ButtonType.submit) {
                            +"Update Student"
                        }
                        div {
                            id = "status"
                        }
                    }
                }
                javascript("students/${student.id}")
            }
        }

    }
}