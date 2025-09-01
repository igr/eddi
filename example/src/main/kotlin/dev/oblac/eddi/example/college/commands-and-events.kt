package dev.oblac.eddi.example.college

import dev.oblac.eddi.Command
import dev.oblac.eddi.Event
import dev.oblac.eddi.Tag
import java.time.Instant

@JvmInline
value class StudentTag(override val id: String) : Tag

// Root event - foundational event for student lifecycle
data class StudentRegistered(
    val student: StudentTag,
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant = Instant.now()
) : Event

// Payment event - depends on StudentRegistered
data class TuitionPaid(
    val student: StudentTag,
    val amount: Double,
    val paidAt: Instant = Instant.now(),
    val semester: String
) : Event

// Enrollment event - depends on StudentRegistered
data class Enrolled(
    val student: StudentTag,
    val course: CourseTag,
    val enrolledAt: Instant = Instant.now()
) : Event

@JvmInline
value class CourseTag(override val id: String) : Tag

// Course publishing event - independent foundational event
data class CoursePublished(
    val course: CourseTag,
    val courseName: String,
    val instructor: String,
    val credits: Int,
    val publishAt: Instant = Instant.now()
) : Event

// Grading event - depends on both Enrolled and CoursePublished
data class Graded(
    val student: StudentTag,
    val course: CourseTag,
    val grade: String, // A, B, C, D, F
    val gradedAt: Instant = Instant.now()
) : Event

// Student departure event - depends on StudentRegistered
data class StudentDeregistered(
    val student: StudentTag,
    val deregisteredAt: Instant = Instant.now(),
    val reason: String? = null
) : Event


// Corresponding Commands
data class RegisterStudent(
    val student: StudentTag,
    val firstName: String,
    val lastName: String,
    val email: String
) : Command

data class PayTuition(
    val student: StudentTag,
    val amount: Double,
    val semester: String
) : Command

data class EnrollInCourse(
    val student: StudentTag,
    val course: CourseTag
) : Command

data class PublishCourse(
    val course: CourseTag,
    val courseName: String,
    val instructor: String,
    val credits: Int
) : Command

data class GradeStudent(
    val student: StudentTag,
    val course: CourseTag,
    val grade: String
) : Command

data class DeregisterStudent(
    val student: StudentTag,
    val reason: String? = null
) : Command