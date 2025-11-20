package dev.oblac.eddi.example.college

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.TTag
import java.time.Instant

// --- student ---

@JvmInline
value class StudentRegisteredTag(override val seq: ULong) : TTag<StudentRegistered>

@JvmName("studentRegisteredRef")
fun EventEnvelope<StudentRegistered>.tag(): StudentRegisteredTag {
    return StudentRegisteredTag(this.sequence)
}

data class StudentRegistered(
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant = Instant.now()
) : Event

// --- tuition ---

@JvmInline
value class TuitionPaidTag(override val seq: ULong) : TTag<TuitionPaid>

@JvmName("tuitionPaidRef")
fun EventEnvelope<TuitionPaid>.tag(): TuitionPaidTag {
    return TuitionPaidTag(this.sequence)
}

// Payment event
data class TuitionPaid(
    val student: StudentRegisteredTag,
    val amount: Double,
    val paidAt: Instant = Instant.now(),
    val semester: String
) : Event


@JvmInline
value class CoursePublishedTag(override val seq: ULong) : TTag<CoursePublished>

@JvmName("coursePublishedRef")
fun EventEnvelope<CoursePublished>.tag(): CoursePublishedTag {
    return CoursePublishedTag(this.sequence)
}

// Course publishing event
data class CoursePublished(
    val courseName: String,
    val instructor: String,
    val credits: Int,
    val publishAt: Instant = Instant.now()
) : Event

@JvmInline
value class EnrolledTag(override val seq: ULong) : TTag<Enrolled>

data class Enrolled(
    val tuitionPaid: TuitionPaidTag,
    val course: CoursePublishedTag,
    val enrolledAt: Instant = Instant.now()
) : Event

@JvmName("enrolledRef")
fun EventEnvelope<Enrolled>.tag(): EnrolledTag {
    return EnrolledTag(this.sequence)
}

data class Graded(
    val enrolled: EnrolledTag,
    val grade: String, // A, B, C, D, F
    val gradedAt: Instant = Instant.now()
) : Event

