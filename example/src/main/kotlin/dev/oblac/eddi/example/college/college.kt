package dev.oblac.eddi.example.college

import dev.oblac.eddi.Event
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
        val student = StudentTag("S001")
        val course = CourseTag("CS101")
        storeCommand(RegisterStudent(student, "John", "Doe", "john.doe@college.edu"))
        storeCommand(PublishCourse(course, "Introduction to Computer Science", "Dr. Smith", 3))
        storeCommand(PayTuition(student, 1500.0, "Fall 2024"))
        storeCommand(EnrollInCourse(student, course))
        storeCommand(GradeStudent(student, course, "A"))
    }

    println("College system initialized with sample commands")
    readln()
}

fun registerStudent(command: RegisterStudent): Array<StudentRegistered> {
    println("🔥 Registering student: ${command.firstName} ${command.lastName}")
    return arrayOf(
        StudentRegistered(
            student = command.student,
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email
        )
    )
}

fun payTuition(command: PayTuition): Array<TuitionPaid> {
    println("🔥 Processing tuition payment for student: ${command.student}")
    return arrayOf(
        TuitionPaid(
            student = command.student,
            amount = command.amount,
            semester = command.semester
        )
    )
}

class EnrollInCourseService(val evetStore: EventStoreRepo) : Service<EnrollInCourse, Enrolled> {
    override fun invoke(command: EnrollInCourse): Array<Enrolled> {
        // 1) check if the student is registered
        evetStore.findLastTaggedEvent(Event.type<StudentRegistered>(), command.student)
            ?: throw IllegalStateException("Student ${command.student} is not registered")    // dont throw exception!

        // 2) check the course is published
        evetStore.findLastTaggedEvent(Event.type<CoursePublished>(), command.course)
            ?: throw IllegalStateException("Course ${command.course} is not published")

        println("🔥 Enrolling student ${command.student} in course ${command.course}")

        return arrayOf(
            Enrolled(
                student = command.student,
                course = command.course
            )
        )
    }
}

fun publishCourse(command: PublishCourse): Array<CoursePublished> {
    println("🔥 Publishing course: ${command.courseName}")
    return arrayOf(
        CoursePublished(
            course = command.course,
            courseName = command.courseName,
            instructor = command.instructor,
            credits = command.credits
        )
    )
}

fun gradeStudent(command: GradeStudent): Array<Graded> {
    println("🔥 Grading ${command.student} in ${command.course}: ${command.grade}")
    return arrayOf(
        Graded(
            student = command.student,
            course = command.course,
            grade = command.grade
        )
    )
}

fun deregisterStudent(command: DeregisterStudent): Array<StudentDeregistered> {
    println("🔥 Processing student quit: ${command.student}")
    return arrayOf(
        StudentDeregistered(
            student = command.student,
            reason = command.reason
        )
    )
}