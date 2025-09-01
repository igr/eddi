package dev.oblac.eddi.example.college

import dev.oblac.eddi.Command
import dev.oblac.eddi.Event
import dev.oblac.eddi.Tag
import java.time.Instant

interface StudentTag : Tag {
    val studentId: String
}

// Root event - foundational event for student lifecycle
data class StudentRegistered(
    override val studentId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant = Instant.now()
) : Event, StudentTag

// Payment event - depends on StudentRegistered
data class TuitionPaid(
    override val studentId: String,
    val amount: Double,
    val paidAt: Instant = Instant.now(),
    val semester: String
) : Event, StudentTag

// Enrollment event - depends on StudentRegistered
data class Enrolled(
    val studentId: String,
    val courseId: String,
    val enrolledAt: Instant = Instant.now()
) : Event

interface CourseTag : Tag {
    val courseId: String
}

// Course publishing event - independent foundational event
data class CoursePublished(
    override val courseId: String,
    val courseName: String,
    val instructor: String,
    val credits: Int,
    val publishAt: Instant = Instant.now()
) : Event, CourseTag

// Grading event - depends on both Enrolled and CoursePublished
data class Graded(
    val studentId: String,
    val courseId: String,
    val grade: String, // A, B, C, D, F
    val gradedAt: Instant = Instant.now()
) : Event

// Student departure event - depends on StudentRegistered
data class StudentDeregistered(
    val studentId: String,
    val deregisteredAt: Instant = Instant.now(),
    val reason: String? = null
) : Event


// Corresponding Commands
data class RegisterStudent(
    val studentId: String,
    val firstName: String,
    val lastName: String,
    val email: String
) : Command

data class PayTuition(
    val studentId: String,
    val amount: Double,
    val semester: String
) : Command

data class EnrollInCourse(
    val studentId: String,
    val courseId: String
) : Command

data class PublishCourse(
    val courseId: String,
    val courseName: String,
    val instructor: String,
    val credits: Int
) : Command

data class GradeStudent(
    val studentId: String,
    val courseId: String,
    val grade: String
) : Command

data class DeregisterStudent(
    val studentId: String,
    val reason: String? = null
) : Command