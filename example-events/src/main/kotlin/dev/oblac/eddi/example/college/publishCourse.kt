package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import dev.oblac.eddi.*
import java.time.Instant
import java.util.UUID

data class PublishCourse(
    val courseName: String,
    val instructor: String,
) : Command

@JvmInline
value class CourseId(override val id: UUID) : Id

data class CoursePublished(
    val courseId: CourseId,
    val courseName: String,
    val instructor: String,
    val publishAt: Instant = Instant.now()
) : Event {
    override fun ids() = listOf(courseId)
}

sealed interface PublishCourseError : CommandError {
    object CourseAlreadyExists : PublishCourseError
}

fun ensureUniqueCourse(es: EventStoreRepo) = commandProcessor<PublishCourse> {
    ensure(
        es.findEvents<CoursePublished>(mapOf("courseName" to it.courseName)).isEmpty()
    ) { PublishCourseError.CourseAlreadyExists }
}

operator fun PublishCourse.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureUniqueCourse(es)
        emit { CoursePublished(CourseId(UUID.randomUUID()), courseName, instructor) }
    }
