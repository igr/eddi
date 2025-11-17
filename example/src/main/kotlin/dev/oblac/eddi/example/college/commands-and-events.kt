package dev.oblac.eddi.example.college

import dev.oblac.eddi.Command
import dev.oblac.eddi.Event
import dev.oblac.eddi.Tag
import java.time.Instant

sealed interface AppEvent : Event

@JvmInline
value class StudentRegisteredId(override val id: String) : Tag

// Root event - foundational event for student lifecycle
data class StudentRegistered(
    val id: StudentRegisteredId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant = Instant.now()
) : AppEvent

@JvmInline
value class TuitionPaidId(override val id: String) : Tag

// Payment event
data class TuitionPaid(
    val id: TuitionPaidId,
    val student: StudentRegisteredId,
    val amount: Double,
    val paidAt: Instant = Instant.now(),
    val semester: String
) : AppEvent

@JvmInline
value class CoursePublishedId(override val id: String) : Tag

// Course publishing event
data class CoursePublished(
    val id: CoursePublishedId,
    val courseName: String,
    val instructor: String,
    val credits: Int,
    val publishAt: Instant = Instant.now()
) : AppEvent

@JvmInline
value class EnrolledId(override val id: String) : Tag

data class Enrolled(
    val id: EnrolledId,
    val tuitionPaid: TuitionPaidId,
    val course: CoursePublishedId,
    val enrolledAt: Instant = Instant.now()
) : AppEvent

@JvmInline
value class GradedId(override val id: String) : Tag

// Grading event - depends on both Enrolled and CoursePublished
data class Graded(
    val id: GradedId,
    val enrolledId: EnrolledId,
    val grade: String, // A, B, C, D, F
    val gradedAt: Instant = Instant.now()
) : AppEvent

@JvmInline
value class StudentDeregisteredId(override val id: String) : Tag

// Student departure event - depends on StudentRegistered
data class StudentDeregistered(
    val id: StudentDeregisteredId,
    val student: StudentRegisteredId,
    val deregisteredAt: Instant = Instant.now(),
    val reason: String? = null
) : Event

/** Corresponding Commands **/

sealed interface AppCommand : Command

data class RegisterStudent(
    val firstName: String,
    val lastName: String,
    val email: String
) : AppCommand

data class PayTuition(
    val student: StudentRegisteredId,
    val amount: Double,
    val semester: String
) : AppCommand

data class EnrollInCourse(
    val tuitionPaid: TuitionPaidId,
    val course: CoursePublishedId,
) : AppCommand

data class PublishCourse(
    val courseName: String,
    val instructor: String,
    val credits: Int
) : AppCommand

data class GradeStudent(
    val enrolled: EnrolledId,
    val grade: String
) : AppCommand

data class DeregisterStudent(
    val student: StudentRegisteredId,
    val reason: String? = null
) : Command