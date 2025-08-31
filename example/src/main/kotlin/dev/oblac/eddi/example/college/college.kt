package dev.oblac.eddi.example.college

import dev.oblac.eddi.EventStoreRepo
import dev.oblac.eddi.Service
import dev.oblac.eddi.example.createMemoryEddie
import dev.oblac.eddi.registerService


fun main() {
    val eddi = createMemoryEddie()

    with(eddi.serviceRegistry) {
        registerService(::registerStudent)
        registerService(::payTuition)
        registerService(EnrollInCourseService(eddi.evetStore))
        registerService(::publishCourse)
        registerService(::gradeStudent)
        registerService(::deregisterStudent)
    }

    createProjections(eddi)

    // USAGE

    with(eddi.commandStore) {
        storeCommand(RegisterStudent("S001", "John", "Doe", "john.doe@college.edu"))
        storeCommand(PublishCourse("CS101", "Introduction to Computer Science", "Dr. Smith", 3))
        storeCommand(PayTuition("S001", 1500.0, "Fall 2024"))
        storeCommand(EnrollInCourse("S001", "CS101"))
        storeCommand(GradeStudent("S001", "CS101", "A"))
    }

    println("College system initialized with sample commands")
    readln()
}

fun registerStudent(command: RegisterStudent): Array<StudentRegistered> {
    println("🔥 Registering student: ${command.firstName} ${command.lastName}")
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
    println("🔥 Processing tuition payment for student: ${command.studentId}")
    return arrayOf(
        TuitionPaid(
            studentId = command.studentId,
            amount = command.amount,
            semester = command.semester
        )
    )
}

class EnrollInCourseService(val evetStore: EventStoreRepo) : Service<EnrollInCourse, Enrolled> {
    override fun invoke(command: EnrollInCourse): Array<Enrolled> {
        // 1) check if the student is registered
        evetStore.findLastTaggedEvent(StudentRegistered::class, StudentId::class, command.studentId)
            ?: throw IllegalStateException("Student ${command.studentId} is not registered")    // dont throw exception!

        // 2) check the course is published
        evetStore.findLastTaggedEvent(CoursePublished::class, CourseId::class, command.courseId)
            ?: throw IllegalStateException("Course ${command.courseId} is not published")

        println("🔥 Enrolling student ${command.studentId} in course ${command.courseId}")

        return arrayOf(
            Enrolled(
                studentId = command.studentId,
                courseId = command.courseId
            )
        )
    }
}

fun publishCourse(command: PublishCourse): Array<CoursePublished> {
    println("🔥 Publishing course: ${command.courseName}")
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
    println("🔥 Grading student ${command.studentId} in course ${command.courseId}: ${command.grade}")
    return arrayOf(
        Graded(
            studentId = command.studentId,
            courseId = command.courseId,
            grade = command.grade
        )
    )
}

fun deregisterStudent(command: DeregisterStudent): Array<StudentDeregistered> {
    println("🔥 Processing student quit: ${command.studentId}")
    return arrayOf(
        StudentDeregistered(
            studentId = command.studentId,
            reason = command.reason
        )
    )
}