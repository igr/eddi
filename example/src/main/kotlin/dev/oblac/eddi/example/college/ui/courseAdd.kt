package dev.oblac.eddi.example.college.ui

import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Routing.pageCourseAdd() {
    get("/course-add.html") {
        call.respondHtml {
            head()
            body {
                div("container") {
                    h1 { +"Add Course" }
                    a (href = "/") { +"Home" }
                    form {
                        id = "apiForm"
                        input(type = InputType.text) {
                            name = "name"
                            placeholder = "Name"
                        }
                        input(type = InputType.text) {
                            name = "instructor"
                            placeholder = "Instructor"
                        }
                        button(type = ButtonType.submit) {
                            +"Add Course"
                        }
                        div {
                            id = "status"
                        }
                    }
                }
                javascript("courses")
            }
        }

    }
}