package dev.oblac.eddi.example.college

import dev.oblac.eddi.Events

/**
 * Registers the college events with the event store, which stores and reads them back by name.
 * Every event declared in this package must be listed here.
 */
fun registerCollegeEvents() {
    Events.register(
        CoursePublished::class,
        StudentEnrolledInCourse::class,
        StudentRegistered::class,
        StudentUpdated::class,
        TuitionPaid::class,
    )
}
