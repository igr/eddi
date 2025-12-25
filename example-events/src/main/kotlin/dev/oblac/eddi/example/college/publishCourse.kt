package dev.oblac.eddi.example.college

import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.oblac.eddi.*
import java.time.Instant

data class PublishCourse(
    val courseName: String,
    val instructor: String,
) : Command

@JvmInline
value class CoursePublishedTag(override val seq: Seq) : Tag<CoursePublished>

data class CoursePublished(
    val courseName: String,
    val instructor: String,
    val publishAt: Instant = Instant.now()
) : Event

sealed interface PublishCourseError : CommandError {
    object CourseAlreadyExists : PublishCourseError
}

fun ensureUniqueCourse(es: EventStoreRepo) = CommandProcessor<PublishCourse> {
    either {
        ensure(
            es.findEvents<CoursePublished>(
                CoursePublishedEvent.NAME,
                mapOf("courseName" to it.courseName)
            ).isEmpty()
        ) { PublishCourseError.CourseAlreadyExists }
        it
    }
}

operator fun PublishCourse.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureUniqueCourse(es)
        emit { CoursePublished(courseName, instructor) }
    }