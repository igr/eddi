package dev.oblac.eddi.example.college

import dev.oblac.eddi.Command
import dev.oblac.eddi.Event
import dev.oblac.eddi.example.createMemoryEddie
import java.time.Instant

// Root event - foundational event for student lifecycle
data class StudentRegistered(
    val studentId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val registeredAt: Instant = Instant.now()
) : Event

// Payment event - depends on StudentRegistered
data class TuitionPaid(
    val studentId: String,
    val amount: Double,
    val paidAt: Instant = Instant.now(),
    val semester: String
) : Event

// Enrollment event - depends on StudentRegistered
data class Enrolled(
    val studentId: String,
    val courseId: String,
    val enrolledAt: Instant = Instant.now()
) : Event

// Course publishing event - independent foundational event
data class CoursePublished(
    val courseId: String,
    val courseName: String,
    val instructor: String,
    val credits: Int,
    val publishAt: Instant = Instant.now()
) : Event

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

fun main() {
    val eddi = createMemoryEddie()

    // Register command handlers following the NoEntities pattern
    with(eddi.serviceRegistry) {
        registerService(RegisterStudent::class, ::registerStudent)
        registerService(PayTuition::class, ::payTuition)
        registerService(EnrollInCourse::class, ::enrollInCourse)
        registerService(PublishCourse::class, ::publishCourse)
        registerService(GradeStudent::class, ::gradeStudent)
        registerService(DeregisterStudent::class, ::deregisterStudent)
    }

    projections(eddi)

    // Example usage
    val registerCmd = RegisterStudent("S001", "John", "Doe", "john.doe@college.edu")
    val publishCmd = PublishCourse("CS101", "Introduction to Computer Science", "Dr. Smith", 3)

    with(eddi.commandStore) {
        storeCommand(registerCmd)
        storeCommand(publishCmd)
    }

    println("College system initialized with sample commands")

    readln()
}

// Command handlers - following NoEntities pattern where commands produce events
fun registerStudent(command: RegisterStudent): Array<StudentRegistered> {
    println("Registering student: ${command.firstName} ${command.lastName}")
    return arrayOf(
        StudentRegistered(
            studentId = command.studentId,
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email
        )
    )
}

fun payTuition(command: PayTuition): Array<TuitionPaid> {
    println("Processing tuition payment for student: ${command.studentId}")
    return arrayOf(
        TuitionPaid(
            studentId = command.studentId,
            amount = command.amount,
            semester = command.semester
        )
    )
}

fun enrollInCourse(command: EnrollInCourse): Array<Enrolled> {
    println("Enrolling student ${command.studentId} in course ${command.courseId}")
    return arrayOf(
        Enrolled(
            studentId = command.studentId,
            courseId = command.courseId
        )
    )
}

fun publishCourse(command: PublishCourse): Array<CoursePublished> {
    println("Publishing course: ${command.courseName}")
    return arrayOf(
        CoursePublished(
            courseId = command.courseId,
            courseName = command.courseName,
            instructor = command.instructor,
            credits = command.credits
        )
    )
}

fun gradeStudent(command: GradeStudent): Array<Graded> {
    println("Grading student ${command.studentId} in course ${command.courseId}: ${command.grade}")
    return arrayOf(
        Graded(
            studentId = command.studentId,
            courseId = command.courseId,
            grade = command.grade
        )
    )
}

fun deregisterStudent(command: DeregisterStudent): Array<StudentDeregistered> {
    println("Processing student quit: ${command.studentId}")
    return arrayOf(
        StudentDeregistered(
            studentId = command.studentId,
            reason = command.reason
        )
    )
}