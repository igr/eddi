package dev.oblac.eddi.example.college

import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import dev.oblac.eddi.*
import java.time.Instant

data class EnrollStudentInCourse(
    val student: StudentRegisteredTag,
    val course: CoursePublishedTag,
) : Command

data class StudentEnrolledInCourse(
    val student: StudentRegisteredTag,
    val course: CoursePublishedTag,
    val enrolledAt: Instant = Instant.now()
) : Event


sealed interface EnrollStudentInCourseError : CommandError {
    data class StudentNotFound(val student: StudentRegisteredTag) : EnrollStudentInCourseError {
        override fun toString(): String = "Student with id ${student.seq} not found"
    }

    data class CourseNotFound(val course: CoursePublishedTag) : EnrollStudentInCourseError {
        override fun toString(): String = "Course with id ${course.seq} not found"
    }

    data class TuitionNotPaid(val student: StudentRegisteredTag) : EnrollStudentInCourseError {
        override fun toString(): String = "Student with id ${student.seq} has not paid tuition"
    }

    data class AlreadyEnrolled(val student: StudentRegisteredTag) : EnrollStudentInCourseError {
        override fun toString(): String = "Student with id ${student.seq} has already been enrolled in the course"
    }
}

fun ensureEnrollStudentExists(es: EventStoreRepo) = CommandProcessor<EnrollStudentInCourse> {
    either {
        ensureNotNull(
            es.findEvent<StudentRegistered>(
                it.student.seq,
                StudentRegisteredEvent.NAME,
            )
        ) { EnrollStudentInCourseError.StudentNotFound(it.student) }
        it
    }
}

fun ensureCourseExists(es: EventStoreRepo) = CommandProcessor<EnrollStudentInCourse> {
    either {
        ensureNotNull(
            es.findEvent<CoursePublished>(
                it.course.seq,
                CoursePublishedEvent.NAME,
            )
        ) { EnrollStudentInCourseError.CourseNotFound(it.course) }
        it
    }
}

fun ensureNotAlreadyEnrolled(es: EventStoreRepo) = CommandProcessor<EnrollStudentInCourse> {
    either {
        ensure(
            es.findEventByMultipleTags<StudentEnrolledInCourse>(
                StudentEnrolledInCourseEvent.NAME,
                StudentRegisteredTag(it.student.seq),
                CoursePublishedTag(it.course.seq)
            ) == null
        ) { EnrollStudentInCourseError.AlreadyEnrolled(it.student) }
        it
    }
}

fun ensureTuitionPaid(es: EventStoreRepo) = CommandProcessor<EnrollStudentInCourse> {
    either {
        ensureNotNull(
            es.findEventByTag(
                TuitionPaidEvent.NAME,
                StudentRegisteredTag(it.student.seq)
            )
        ) { EnrollStudentInCourseError.TuitionNotPaid(it.student) }
        it
    }
}

operator fun EnrollStudentInCourse.invoke(es: EventStoreRepo) =
    process(this) {
        +ensureEnrollStudentExists(es)
        +ensureCourseExists(es)
        +ensureNotAlreadyEnrolled(es)
        +ensureTuitionPaid(es)
        emit { StudentEnrolledInCourse(student, course) }
    }