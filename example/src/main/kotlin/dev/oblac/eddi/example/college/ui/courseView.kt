package dev.oblac.eddi.example.college.ui

import dev.oblac.eddi.example.college.projection.dbFindCourseById
import dev.oblac.eddi.example.college.projection.dbFindCourseStudents
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*
import java.util.*

fun Routing.pageCourseView() {
    get("/course.html") {
        val courseId = call.parameters["id"]
        if (courseId == null) {
            call.respondHtml {
                head()
                body {
                    div("container") {
                        h1 { +"Error" }
                        a (href = "/") { +"Home" }
                        p { +"Missing Course ID." }
                    }
                }
            }
            return@get
        }
        val courseUUID = UUID.fromString(courseId)
        val course = dbFindCourseById(courseUUID)!!
        val courseStudents = dbFindCourseStudents(courseUUID)
        call.respondHtml {
            head()
            body {
                div("container") {
                    h1 { +"Course: ${course.name} by ${course.instructor}" }
                    a (href = "/courses.html") { +"All Courses" }

                    h2 { +"Enrolled Students" }
                    table {
                        thead {
                            tr {
                                th { +"Name" }
                            }
                        }
                        tbody {
                            for (student in courseStudents.enrolled) {
                                tr {
                                    td { +"${student.firstName} ${student.lastName}" }
                                }
                            }
                        }
                    }

                    h2 { +"Not Enrolled Students" }
                    form {
                        id = "apiForm"
                        table {
                            thead {
                                tr {
                                    th { +"Name" }
                                    th { +"Actions" }
                                }
                            }
                            tbody {
                                for (student in courseStudents.notEnrolled) {
                                    tr {
                                        td { +"${student.firstName} ${student.lastName}" }
                                        td {
                                            button(type = ButtonType.submit) {
                                                attributes["data-studentId"] = student.id.toString()
                                                attributes["data-courseId"] = course.id.toString()
                                                +"Enroll"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                div {
                    id = "status"
                }
                javascript("enrolls")
            }
        }
    }
}
