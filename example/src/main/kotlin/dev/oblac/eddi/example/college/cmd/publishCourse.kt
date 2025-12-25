package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.example.college.CoursePublished
import dev.oblac.eddi.example.college.CoursePublishedEvent
import dev.oblac.eddi.example.college.PublishCourse
import dev.oblac.eddi.process

object CourseAlreadyExistsError : CommandError {
    override fun toString(): String = "Course already exists"
}

fun ensureUniqueCourse(es: EventStore): (PublishCourse) -> Either<CourseAlreadyExistsError, PublishCourse> =
    {
        either {
            ensure(
                es.findEvents<CoursePublished>(
                    CoursePublishedEvent.NAME,
                    mapOf("courseName" to it.courseName)
                ).isEmpty()
            ) { CourseAlreadyExistsError }
            it
        }
    }

operator fun PublishCourse.invoke(es: EventStore) =
    process(this) {
        +ensureUniqueCourse(es)
        emit { CoursePublished(courseName, instructor) }
    }