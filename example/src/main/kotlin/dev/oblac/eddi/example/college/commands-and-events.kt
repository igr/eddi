package dev.oblac.eddi.example.college

import dev.oblac.eddi.*
import java.time.Instant

sealed interface AppEvent : Event

@JvmInline
value class StudentRegisteredRef(override val seq: ULong) : Tag {
    override val name get() = StudentRegistered.NAME
}

@JvmName("studentRegisteredRef")
fun EventEnvelope<StudentRegistered>.ref(): StudentRegisteredRef {
    return StudentRegisteredRef(this.sequence)
}

// Root event - foundational event for student lifecycle
data class StudentRegistered(
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant = Instant.now()
) : AppEvent {
    companion object {
        val NAME = EventName.of(StudentRegistered::class)
    }
}

@JvmInline
value class TuitionPaidRef(override val seq: ULong) : Tag {
    override val name get() = TuitionPaid.NAME
}

@JvmName("tuitionPaidRef")
fun EventEnvelope<TuitionPaid>.ref(): TuitionPaidRef {
    return TuitionPaidRef(this.sequence)
}

// Payment event
data class TuitionPaid(
    val student: StudentRegisteredRef,
    val amount: Double,
    val paidAt: Instant = Instant.now(),
    val semester: String
) : AppEvent {
    companion object {
        val NAME = EventName.of(TuitionPaid::class)
    }
}


@JvmInline
value class CoursePublishedRef(override val seq: ULong) : Tag {
    override val name get() = CoursePublished.NAME
}

@JvmName("coursePublishedRef")
fun EventEnvelope<CoursePublished>.ref(): CoursePublishedRef {
    return CoursePublishedRef(this.sequence)
}

// Course publishing event
data class CoursePublished(
    val courseName: String,
    val instructor: String,
    val credits: Int,
    val publishAt: Instant = Instant.now()
) : AppEvent {
    companion object {
        val NAME = EventName.of(CoursePublished::class)
    }
}

@JvmInline
value class EnrolledRef(override val seq: ULong) : Tag {
    override val name get() = Enrolled.NAME
}

data class Enrolled(
    val tuitionPaid: TuitionPaidRef,
    val course: CoursePublishedRef,
    val enrolledAt: Instant = Instant.now()
) : AppEvent {
    companion object {
        val NAME = EventName.of(Enrolled::class)
    }
}

@JvmName("enrolledRef")
fun EventEnvelope<Enrolled>.ref(): EnrolledRef {
    return EnrolledRef(this.sequence)
}

// Grading event - depends on both Enrolled and CoursePublished
data class Graded(
    val enrolled: EnrolledRef,
    val grade: String, // A, B, C, D, F
    val gradedAt: Instant = Instant.now()
) : AppEvent {
    companion object {
        val NAME = EventName.of(Graded::class)
    }
}

/** Corresponding Commands **/

sealed interface AppCommand : Command

data class RegisterStudent(
    val firstName: String,
    val lastName: String,
    val email: String
) : AppCommand

data class PayTuition(
    val student: StudentRegisteredRef,
    val amount: Double,
    val semester: String
) : AppCommand

data class EnrollInCourse(
    val tuitionPaid: TuitionPaidRef,
    val course: CoursePublishedRef,
) : AppCommand

data class PublishCourse(
    val courseName: String,
    val instructor: String,
    val credits: Int
) : AppCommand

data class GradeStudent(
    val enrolled: EnrolledRef,
    val grade: String
) : AppCommand

data class DeregisterStudent(
    val student: StudentRegisteredRef,
    val reason: String? = null
) : Command