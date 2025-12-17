package dev.oblac.eddi.example.college.cmd

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import dev.oblac.eddi.CommandError
import dev.oblac.eddi.example.college.CoursePublished
import dev.oblac.eddi.example.college.PublishCourse

object PublishNewCourseError : CommandError {
    override fun toString(): String = "Course already exists"
}

fun publishCourse(
    command: PublishCourse,
    courseExists: (String) -> Boolean
): Either<CommandError, CoursePublished> =
    command.right()
        .flatMap { uniqueCourseName(it, courseExists) }
        .map {
            CoursePublished(
                courseName = it.courseName,
                instructor = it.instructor
            )
        }


private fun uniqueCourseName(
    command: PublishCourse,
    courseExist: (String) -> Boolean
): Either<PublishNewCourseError, PublishCourse> = either {
    ensure(!courseExist(command.courseName)) {
        println("Course ${command.courseName} already exists")
        PublishNewCourseError
    }
    command
}