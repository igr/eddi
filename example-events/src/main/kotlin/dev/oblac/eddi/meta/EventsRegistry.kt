package dev.oblac.eddi.meta

import dev.oblac.eddi.Events
import dev.oblac.eddi.example.college.CoursePublishedEvent
import dev.oblac.eddi.example.college.CoursePublishedTag
import dev.oblac.eddi.example.college.StudentEnrolledInCourseEvent
import dev.oblac.eddi.example.college.StudentRegisteredEvent
import dev.oblac.eddi.example.college.StudentRegisteredTag
import dev.oblac.eddi.example.college.StudentUpdatedEvent
import dev.oblac.eddi.example.college.StudentUpdatedTag
import dev.oblac.eddi.example.college.TuitionPaidEvent

object EventsRegistry {
    fun init() {
        // Register EventMeta implementations
        Events.register(
            listOf(
                CoursePublishedEvent,
                StudentEnrolledInCourseEvent,
                StudentRegisteredEvent,
                StudentUpdatedEvent,
                TuitionPaidEvent,
            )
        )
        // Register Tag implementations
        Events.register(CoursePublishedTag::class, CoursePublishedEvent.NAME)
        Events.register(StudentRegisteredTag::class, StudentRegisteredEvent.NAME)
        Events.register(StudentUpdatedTag::class, StudentUpdatedEvent.NAME)
    }
}
