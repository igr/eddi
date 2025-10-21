package dev.oblac.eddi.example.college

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventListener

data class StudentView(
    val id: StudentRegisteredId,
    val name: String,
    var tuitionPaid: Boolean = false
)

data class CourseView(
    val id: CoursePublishedId,
    val courseName: String,
)

object Projections : EventListener {
    val students = mutableMapOf<StudentRegisteredId, StudentView>()
    val courses = mutableMapOf<CoursePublishedId, CourseView>()

    override fun invoke(ee: EventEnvelope<Event>) {
        when (val event = ee.event as AppEvent) {
            is StudentRegistered -> {
                students[event.id] = StudentView(event.id, "${event.firstName} ${event.lastName}")
                println("🗃️ Student registered: ${event.firstName} ${event.lastName} (${event.email})")
            }
            is TuitionPaid -> {
                students[event.student]!!.tuitionPaid = true
                println("🗃️ Tuition paid: ${event.student} Amount: ${event.amount} for ${event.semester}")
            }
            is CoursePublished -> {
                courses[event.id] = CourseView(event.id, event.courseName)
                println("🗃️ Course published: ${event.courseName} by ${event.instructor}")
            }
            else -> Unit
        }
    }


}