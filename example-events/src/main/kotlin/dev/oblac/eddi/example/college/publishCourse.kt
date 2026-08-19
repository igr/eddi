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

/**
 * Value-derived id of a course name: the same name always yields the same [CourseNameId], so
 * "a course with this name exists" is an id lookup inside the consistency boundary.
 */
@JvmInline
value class CourseNameId(override val id: UUID) : Id {
    companion object {
        fun of(courseName: String) = CourseNameId(UUID.nameUUIDFromBytes("courseName:$courseName".toByteArray()))
    }
}

data class CoursePublished(
    val courseId: CourseId,
    val courseName: String,
    val instructor: String,
    val publishAt: Instant = Instant.now()
) : Event {
    override fun ids() = listOf(courseId, CourseNameId.of(courseName))
}

sealed interface PublishCourseError : CommandError {
    object CourseAlreadyExists : PublishCourseError {
        override fun toString(): String = "Course with this name already exists"
    }
}

fun ensureUniqueCourse(es: EventStoreRepo) = commandProcessor<PublishCourse> {
    ensure(
        es.findEventById<CoursePublished>(CourseNameId.of(it.courseName)) == null
    ) { PublishCourseError.CourseAlreadyExists }
}

operator fun PublishCourse.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureUniqueCourse(es)
        emit { CoursePublished(CourseId(UUID.randomUUID()), courseName, instructor) }
    }
