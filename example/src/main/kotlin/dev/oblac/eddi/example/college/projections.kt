package dev.oblac.eddi.example.college

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventListener

data class StudentView(
    val id: ULong,
    val name: String,
    var tuitionPaid: Boolean = false
)

data class CourseView(
    val id: ULong,
    val courseName: String,
)

object Projections : EventListener {
    val students = mutableMapOf<ULong, StudentView>()
    val courses = mutableMapOf<ULong, CourseView>()

    override fun invoke(ee: EventEnvelope<Event>) {
        when (val event = ee.event as AppEvent) {
            is StudentRegistered -> {
                students[ee.sequence] = StudentView(ee.sequence, "${event.firstName} ${event.lastName}")
                println("🗃️ Student registered: ${event.firstName} ${event.lastName} (${event.email})")
            }
            is TuitionPaid -> {
                students[event.student.ref().sequence]!!.tuitionPaid = true
                println("🗃️ Tuition paid: ${event.student} Amount: ${event.amount} for ${event.semester}")
            }
            is CoursePublished -> {
                courses[ee.sequence] = CourseView(ee.sequence, event.courseName)
                println("🗃️ Course published: ${event.courseName} by ${event.instructor}")
            }
            else -> Unit
        }
    }


}