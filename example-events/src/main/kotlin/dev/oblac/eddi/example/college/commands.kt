package dev.oblac.eddi.example.college

import dev.oblac.eddi.Command

/** Corresponding Commands **/

sealed interface AppCommand : Command

data class RegisterStudent(
    val firstName: String,
    val lastName: String,
    val email: String
) : AppCommand

data class UpdateStudent(
    val student: StudentRegisteredTag,
    val firstName: String?,
    val lastName: String?
) : AppCommand

data class PayTuition(
    val student: StudentRegisteredTag,
    val amount: Double,
    val semester: String
) : AppCommand

data class EnrollInCourse(
    val tuitionPaid: TuitionPaidTag,
    val course: CoursePublishedTag,
) : AppCommand

data class PublishCourse(
    val courseName: String,
    val instructor: String,
    val credits: Int
) : AppCommand

data class GradeStudent(
    val enrolled: EnrolledTag,
    val grade: String
) : AppCommand

data class DeregisterStudent(
    val student: StudentRegisteredTag,
    val reason: String? = null
) : Command