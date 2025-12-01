package dev.oblac.eddi.example.college.ui

import dev.oblac.eddi.example.college.projection.dbListStudents
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Routing.pageStudents() {
    get("/students.html") {
        val students = dbListStudents()
        call.respondHtml {
            head()
            body {
                div("container") {
                    h1 { +"Students" }
                    a (href = "/") { +"Home" }
                    table {
                        thead {
                            tr {
                                th { +"First Name" }
                                th { +"Last Name" }
                                th { +"Email" }
                                th { +"Registered At" }
                            }
                        }
                        tbody {
                            for (student in students) {
                                tr {
                                    td { +student.firstName }
                                    td { +student.lastName }
                                    td { +student.email }
                                    td { +student.registeredAt.toString() }
                                }
                            }
                        }
                    }
                }
                javascript("students")
            }
        }
    }
}
