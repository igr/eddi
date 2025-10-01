package dev.oblac.eddi.example.college

import dev.oblac.eddi.Command
import dev.oblac.eddi.Event
import dev.oblac.eddi.Tag
import java.time.Instant

@JvmInline
value class StudentRegisteredId(override val id: String) : Tag

// Root event - foundational event for student lifecycle
data class StudentRegistered(
    val id: StudentRegisteredId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant = Instant.now()
) : Event

@JvmInline
value class TuitionPaidId(override val id: String) : Tag

// Payment event
data class TuitionPaid(
    val id: TuitionPaidId,
    val student: StudentRegisteredId,
    val amount: Double,
    val paidAt: Instant = Instant.now(),
    val semester: String
) : Event

@JvmInline
value class CoursePublishedId(override val id: String) : Tag

// Course publishing event
data class CoursePublished(
    val id: CoursePublishedId,
    val courseName: String,
    val instructor: String,
    val credits: Int,
    val publishAt: Instant = Instant.now()
) : Event

@JvmInline
value class EnrolledId(override val id: String) : Tag

data class Enrolled(
    val id: EnrolledId,
    val tuitionPaidId: TuitionPaidId,
    val courseId: CoursePublishedId,
    val enrolledAt: Instant = Instant.now()
) : Event

@JvmInline
value class GradedId(override val id: String) : Tag

// Grading event - depends on both Enrolled and CoursePublished
data class Graded(
    val id: GradedId,
    val enrolledId: EnrolledId,
    val grade: String, // A, B, C, D, F
    val gradedAt: Instant = Instant.now()
) : Event

@JvmInline
value class StudentDeregisteredId(override val id: String) : Tag

// Student departure event - depends on StudentRegistered
data class StudentDeregistered(
    val id: StudentDeregisteredId,
    val studentRegistered: StudentRegistered,
    val deregisteredAt: Instant = Instant.now(),
    val reason: String? = null
) : Event

/** Corresponding Commands **/

data class RegisterStudent(
    val firstName: String,
    val lastName: String,
    val email: String
) : Command

data class PayTuition(
    val student: StudentRegisteredId,
    val amount: Double,
    val semester: String
) : Command

data class EnrollInCourse(
    val tuitionPaid: TuitionPaidId,
    val course: CoursePublishedId,
) : Command

data class PublishCourse(
    val courseName: String,
    val instructor: String,
    val credits: Int
) : Command

data class GradeStudent(
    val enrolled: EnrolledId,
    val grade: String
) : Command

data class DeregisterStudent(
    val student: StudentRegisteredId,
    val reason: String? = null
) : Command