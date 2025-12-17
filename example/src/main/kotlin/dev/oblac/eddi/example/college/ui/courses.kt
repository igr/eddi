package dev.oblac.eddi.example.college.ui

import dev.oblac.eddi.example.college.projection.dbListCourses
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Routing.pageCourses() {
    get("/courses.html") {
        val courses = dbListCourses()
        call.respondHtml {
            head()
            body {
                div("container") {
                    h1 { +"Courses" }
                    a (href = "/") { +"Home" }
                    table {
                        thead {
                            tr {
                                th { +"Name" }
                                th { +"Instructor" }
                                th { +"Created At" }
                            }
                        }
                        tbody {
                            for (course in courses) {
                                tr {
                                    td { +course.name }
                                    td { +course.instructor }
                                    td { +course.createdAt.toString() }
                                }
                            }
                        }
                    }
                }
                javascript("courses")
            }
        }
    }
}
