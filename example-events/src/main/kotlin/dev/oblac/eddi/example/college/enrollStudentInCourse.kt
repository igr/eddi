package dev.oblac.eddi.example.college

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import dev.oblac.eddi.*
import java.time.Instant

data class EnrollStudentInCourse(
    val student: StudentId,
    val course: CourseId,
) : Command

data class StudentEnrolledInCourse(
    val student: StudentId,
    val course: CourseId,
    val enrolledAt: Instant = Instant.now()
) : Event {
    override fun ids() = listOf(student, course)
}


sealed interface EnrollStudentInCourseError : CommandError {
    data class StudentNotFound(val student: StudentId) : EnrollStudentInCourseError {
        override fun toString(): String = "Student with id ${student.id} not found"
    }

    data class CourseNotFound(val course: CourseId) : EnrollStudentInCourseError {
        override fun toString(): String = "Course with id ${course.id} not found"
    }

    data class TuitionNotPaid(val student: StudentId) : EnrollStudentInCourseError {
        override fun toString(): String = "Student with id ${student.id} has not paid tuition"
    }

    data class AlreadyEnrolled(val student: StudentId) : EnrollStudentInCourseError {
        override fun toString(): String = "Student with id ${student.id} has already been enrolled in the course"
    }
}

fun ensureEnrollStudentExists(es: EventStoreRepo) = commandProcessor<EnrollStudentInCourse> {
    ensureNotNull(
        es.findEventById<StudentRegistered>(it.student)
    ) { EnrollStudentInCourseError.StudentNotFound(it.student) }
}

fun ensureCourseExists(es: EventStoreRepo) = commandProcessor<EnrollStudentInCourse> {
    ensureNotNull(
        es.findEventById<CoursePublished>(it.course)
    ) { EnrollStudentInCourseError.CourseNotFound(it.course) }
}


fun ensureNotAlreadyEnrolled(es: EventStoreRepo) = commandProcessor<EnrollStudentInCourse> {
    ensure(
        es.findEventByMultipleIds<StudentEnrolledInCourse>(it.student, it.course) == null
    ) { EnrollStudentInCourseError.AlreadyEnrolled(it.student) }
}


fun ensureTuitionPaid(es: EventStoreRepo) = commandProcessor<EnrollStudentInCourse> {
    ensureNotNull(
        es.findEventById<TuitionPaid>(it.student)
    ) { EnrollStudentInCourseError.TuitionNotPaid(it.student) }
}


operator fun EnrollStudentInCourse.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureEnrollStudentExists(es)
        +ensureCourseExists(es)
        +ensureNotAlreadyEnrolled(es)
        +ensureTuitionPaid(es)
        emit { StudentEnrolledInCourse(student, course) }
    }
