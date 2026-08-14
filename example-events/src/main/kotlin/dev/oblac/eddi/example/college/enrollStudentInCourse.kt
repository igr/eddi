package dev.oblac.eddi.example.college

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

fun ensureEnrollStudentExists(es: EventStoreRepo) = commandProcessor<EnrollStudentInCourse> {
    ensureNotNull(
        es.findEvent<StudentRegistered>(
            it.student.seq,
            StudentRegisteredEvent.NAME,
        )
    ) { EnrollStudentInCourseError.StudentNotFound(it.student) }
}

fun ensureCourseExists(es: EventStoreRepo) = commandProcessor<EnrollStudentInCourse> {
    ensureNotNull(
        es.findEvent<CoursePublished>(
            it.course.seq,
            CoursePublishedEvent.NAME,
        )
    ) { EnrollStudentInCourseError.CourseNotFound(it.course) }
}


fun ensureNotAlreadyEnrolled(es: EventStoreRepo) = commandProcessor<EnrollStudentInCourse> {
    ensure(
        es.findEventByMultipleTags<StudentEnrolledInCourse>(
            StudentEnrolledInCourseEvent.NAME,
            StudentRegisteredTag(it.student.seq),
            CoursePublishedTag(it.course.seq)
        ) == null
    ) { EnrollStudentInCourseError.AlreadyEnrolled(it.student) }
}


fun ensureTuitionPaid(es: EventStoreRepo) = commandProcessor<EnrollStudentInCourse> {
    ensureNotNull(
        es.findEventByTag(
            TuitionPaidEvent.NAME,
            StudentRegisteredTag(it.student.seq)
        )
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

/**
 * Meta companion class for [StudentEnrolledInCourse].
 */
object StudentEnrolledInCourseEvent : EventMeta<StudentEnrolledInCourse> {

    override val CLASS = StudentEnrolledInCourse::class
    override val NAME = EventName.of(CLASS)

    override fun refs(event: StudentEnrolledInCourse): Array<Ref> = arrayOf(
        Ref(StudentRegisteredEvent.NAME, event.student.seq),
        Ref(CoursePublishedEvent.NAME, event.course.seq)
    )
}
