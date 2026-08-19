package dev.oblac.eddi.example.college.api

import dev.oblac.eddi.ConcurrencyConflict
import dev.oblac.eddi.UnknownCommandError
import dev.oblac.eddi.example.college.*
import io.ktor.http.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class CommandErrorHttpTest {

    private val studentId = StudentId(UUID.randomUUID())
    private val courseId = CourseId(UUID.randomUUID())

    @Test
    fun `not found errors map to 404`() {
        assertEquals(HttpStatusCode.NotFound, UpdateStudentError.StudentNotFound.httpStatus())
        assertEquals(HttpStatusCode.NotFound, PayTuitionError.StudentNotFound.httpStatus())
        assertEquals(HttpStatusCode.NotFound, EnrollStudentInCourseError.StudentNotFound(studentId).httpStatus())
        assertEquals(HttpStatusCode.NotFound, EnrollStudentInCourseError.CourseNotFound(courseId).httpStatus())
    }

    @Test
    fun `state conflicts map to 409`() {
        assertEquals(HttpStatusCode.Conflict, RegisterStudentError.StudentAlreadyExist.httpStatus())
        assertEquals(HttpStatusCode.Conflict, PublishCourseError.CourseAlreadyExists.httpStatus())
        assertEquals(HttpStatusCode.Conflict, PayTuitionError.TuitionAlreadyPaid.httpStatus())
        assertEquals(HttpStatusCode.Conflict, EnrollStudentInCourseError.AlreadyEnrolled(studentId).httpStatus())
        assertEquals(HttpStatusCode.Conflict, EnrollStudentInCourseError.TuitionNotPaid(studentId).httpStatus())
        assertEquals(HttpStatusCode.Conflict, ConcurrencyConflict(setOf(studentId)).httpStatus())
    }

    @Test
    fun `invalid input maps to 422 and unknown commands to 500`() {
        assertEquals(HttpStatusCode.UnprocessableEntity, UpdateStudentError.NothingToUpdate.httpStatus())
        assertEquals(HttpStatusCode.InternalServerError, UnknownCommandError(PayTuition(studentId)).httpStatus())
    }

    @Test
    fun `error bodies carry the error type and a human readable message`() {
        assertEquals(
            ErrorResponse("StudentAlreadyExist", "Student with this email already exists"),
            RegisterStudentError.StudentAlreadyExist.toErrorResponse()
        )
        assertEquals(
            ErrorResponse("TuitionAlreadyPaid", "Tuition has already been paid"),
            PayTuitionError.TuitionAlreadyPaid.toErrorResponse()
        )
        assertEquals(
            ErrorResponse("StudentNotFound", "Student not found"),
            PayTuitionError.StudentNotFound.toErrorResponse()
        )
        assertEquals(
            ErrorResponse("CourseAlreadyExists", "Course with this name already exists"),
            PublishCourseError.CourseAlreadyExists.toErrorResponse()
        )
        assertEquals(
            ErrorResponse("ConcurrencyConflict", "Concurrent modification, please retry"),
            ConcurrencyConflict(setOf(studentId)).toErrorResponse()
        )
    }
}
