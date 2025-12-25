package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.example.college.CoursePublished
import dev.oblac.eddi.example.college.CoursePublishedEvent
import dev.oblac.eddi.example.college.CoursePublishedTag
import dev.oblac.eddi.example.college.EnrollStudentInCourse
import dev.oblac.eddi.example.college.StudentEnrolledInCourse
import dev.oblac.eddi.example.college.StudentEnrolledInCourseEvent
import dev.oblac.eddi.example.college.StudentRegistered
import dev.oblac.eddi.example.college.StudentRegisteredEvent
import dev.oblac.eddi.example.college.StudentRegisteredTag
import dev.oblac.eddi.example.college.TuitionPaidEvent
import dev.oblac.eddi.process

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

fun ensureEnrollStudentExists(es: EventStore): (EnrollStudentInCourse) -> Either<EnrollStudentInCourseError.StudentNotFound, EnrollStudentInCourse> =
    {
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

fun ensureCourseExists(es: EventStore): (EnrollStudentInCourse) -> Either<EnrollStudentInCourseError.CourseNotFound, EnrollStudentInCourse> =
    {
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

fun ensureNotAlreadyEnrolled(es: EventStore): (EnrollStudentInCourse) -> Either<EnrollStudentInCourseError.AlreadyEnrolled, EnrollStudentInCourse> =
    {
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

fun ensureTuitionPaid(es: EventStore): (EnrollStudentInCourse) -> Either<EnrollStudentInCourseError.TuitionNotPaid, EnrollStudentInCourse> =
    {
        either {
            ensure(
                es.findEventByTag(
                    TuitionPaidEvent.NAME,
                    StudentRegisteredTag(it.student.seq)
                ) != null
            ) { EnrollStudentInCourseError.TuitionNotPaid(it.student) }
            it
        }
    }

operator fun EnrollStudentInCourse.invoke(es: EventStore) =
    process(this) {
        +ensureEnrollStudentExists(es)
        +ensureCourseExists(es)
        +ensureNotAlreadyEnrolled(es)
        +ensureTuitionPaid(es)
        emit { StudentEnrolledInCourse(student, course) }
    }