package dev.oblac.eddi.example.college

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventListener
import dev.oblac.eddi.Seq
import dev.oblac.eddi.db.DbEventProcessor

data class StudentView(
    val id: Seq,
    val name: String,
    var tuitionPaid: Boolean = false,
    val courses: MutableList<Seq> = mutableListOf()
)

data class CourseView(
    val id: Seq,
    val courseName: String,
)

object Projections : EventListener {
    private val dbEventProcessor = DbEventProcessor(100L)
    val students = mutableMapOf<Seq, StudentView>()
    val courses = mutableMapOf<Seq, CourseView>()

    override fun invoke(ee: EventEnvelope<Event>) {
        when (val event = ee.event) {
            is StudentRegistered -> {
                students[ee.sequence] = StudentView(ee.sequence, "${event.firstName} ${event.lastName}")
                println("🗃️ Student registered: ${event.firstName} ${event.lastName} (${event.email})")
            }
            is TuitionPaid -> {
                students[event.student.seq]!!.tuitionPaid = true
                println("🗃️ Tuition paid: ${event.student} Amount: ${event.amount} for ${event.semester}")
            }
            is CoursePublished -> {
                courses[ee.sequence] = CourseView(ee.sequence, event.courseName)
                println("🗃️ Course published: ${event.courseName} by ${event.instructor}")
            }
            else -> Unit
        }
    }

    fun start() {
        dbEventProcessor.startInbox(this)
        println("🚀 Projections started")
    }

}