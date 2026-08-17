package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import dev.oblac.eddi.*
import java.time.Instant
import java.util.UUID

data class PublishCourse(
    val courseId: CoursePublishedTag,
    val courseName: String,
    val instructor: String,
) : Command

@JvmInline
value class CoursePublishedTag(override val id: UUID) : Tag<CoursePublished>

data class CoursePublished(
    val courseId: CoursePublishedTag,
    val courseName: String,
    val instructor: String,
    val publishAt: Instant = Instant.now()
) : Event

sealed interface PublishCourseError : CommandError {
    object CourseAlreadyExists : PublishCourseError
}

fun ensureUniqueCourse(es: EventStoreRepo) = commandProcessor<PublishCourse> {
    ensure(
        es.findEvents<CoursePublished>(
            CoursePublishedEvent.NAME,
            mapOf("courseName" to it.courseName)
        ).isEmpty()
    ) { PublishCourseError.CourseAlreadyExists }
}

operator fun PublishCourse.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureUniqueCourse(es)
        emit { CoursePublished(courseId, courseName, instructor) }
    }

/**
 * Meta companion class for [CoursePublished].
 */
object CoursePublishedEvent : EventMeta<CoursePublished> {

    override val CLASS = CoursePublished::class
    override val NAME = EventName.of(CLASS)

    override fun refs(event: CoursePublished): Array<Ref> = arrayOf(
        Ref(NAME, event.courseId.id)
    )
}

fun EventEnvelope<CoursePublished>.tag() = event.courseId
