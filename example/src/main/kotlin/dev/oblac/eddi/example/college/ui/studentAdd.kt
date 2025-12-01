package dev.oblac.eddi.example.college.ui

import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Routing.pageStudentAdd() {
    get("/student-add.html") {
        call.respondHtml {
            head()
            body {
                div("container") {
                    h1 { +"Add Student" }
                    a (href = "/") { +"Home" }
                    form {
                        id = "apiForm"
                        input(type = InputType.text) {
                            name = "firstName"
                            placeholder = "First Name"
                        }
                        input(type = InputType.text) {
                            name = "lastName"
                            placeholder = "Last Name"
                        }
                        button(type = ButtonType.submit) {
                            +"Add Student"
                        }
                        div {
                            id = "status"
                        }
                    }
                }
                javascript("students")
            }
        }

    }
}