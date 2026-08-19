package dev.oblac.eddi.example.college

import dev.oblac.eddi.EventName
import dev.oblac.eddi.Events
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RegisterCollegeEventsTest {

    @Test
    fun `every college event can be resolved back from its name`() {
        registerCollegeEvents()

        listOf(
            CoursePublished::class,
            StudentEnrolledInCourse::class,
            StudentRegistered::class,
            StudentUpdated::class,
            TuitionPaid::class,
        ).forEach { event ->
            assertEquals(event, Events.classOf(EventName.of(event)))
        }
    }
}
