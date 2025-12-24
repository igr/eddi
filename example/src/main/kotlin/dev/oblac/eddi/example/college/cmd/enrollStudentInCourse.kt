package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.example.college.CoursePublishedTag
import dev.oblac.eddi.example.college.EnrollStudentInCourse
import dev.oblac.eddi.example.college.StudentEnrolledInCourse
import dev.oblac.eddi.example.college.StudentRegisteredTag

sealed interface EnrollStudentInCourseError : CommandError {
    data class StudentNotFound(val student: StudentRegisteredTag) : EnrollStudentInCourseError {
        override fun toString(): String = "Student with id ${student.seq} not found"
    }

    data class CourseNotFound(val course: CoursePublishedTag) : EnrollStudentInCourseError {
        override fun toString(): String = "Course with id ${course.seq} not found"
    }

    class TuitionNotPayed(val student: StudentRegisteredTag) : EnrollStudentInCourseError {
        override fun toString(): String =
            "Student with id ${student.seq} has not payed tuition"
    }

    class AlreadyEnrolled(val student: StudentRegisteredTag) : EnrollStudentInCourseError {
        override fun toString(): String =
            "Student with id ${student.seq} has already been enrolled in the course"
    }
}

fun enrollStudentInCourse(
    command: EnrollStudentInCourse,
    studentExists: (StudentRegisteredTag) -> Boolean,
    courseExists: (CoursePublishedTag) -> Boolean,
    unique: (StudentRegisteredTag, CoursePublishedTag) -> Boolean,
    tuitionPaid: (StudentRegisteredTag) -> Boolean,
): Either<CommandError, StudentEnrolledInCourse> {
    if (!studentExists(command.student)) {
        return EnrollStudentInCourseError.StudentNotFound(command.student).left()
    }
    if (!courseExists(command.course)) {
        return EnrollStudentInCourseError.CourseNotFound(command.course).left()
    }
    if (!unique(command.student, command.course)) {
        return EnrollStudentInCourseError.AlreadyEnrolled(command.student).left()
    }
    if (!tuitionPaid(command.student)) {
        return EnrollStudentInCourseError.TuitionNotPayed(command.student).left()
    }
    return StudentEnrolledInCourse(
        student = command.student,
        course = command.course
    ).right()
}