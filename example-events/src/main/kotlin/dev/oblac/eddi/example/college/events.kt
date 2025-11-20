package dev.oblac.eddi.example.college

import dev.oblac.eddi.Event
import dev.oblac.eddi.Tag
import java.time.Instant

@JvmInline
value class StudentRegisteredTag(override val seq: ULong) : Tag<StudentRegistered>

data class StudentRegistered(
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant = Instant.now()
) : Event


@JvmInline
value class TuitionPaidTag(override val seq: ULong) : Tag<TuitionPaid>

data class TuitionPaid(
    val student: StudentRegisteredTag,
    val amount: Double,
    val paidAt: Instant = Instant.now(),
    val semester: String
) : Event


@JvmInline
value class CoursePublishedTag(override val seq: ULong) : Tag<CoursePublished>

data class CoursePublished(
    val courseName: String,
    val instructor: String,
    val credits: Int,
    val publishAt: Instant = Instant.now()
) : Event

@JvmInline
value class EnrolledTag(override val seq: ULong) : Tag<Enrolled>

data class Enrolled(
    val tuitionPaid: TuitionPaidTag,
    val course: CoursePublishedTag,
    val enrolledAt: Instant = Instant.now()
) : Event

data class Graded(
    val enrolled: EnrolledTag,
    val grade: String, // A, B, C, D, F
    val gradedAt: Instant = Instant.now()
) : Event

