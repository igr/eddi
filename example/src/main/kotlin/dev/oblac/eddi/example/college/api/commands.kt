package dev.oblac.eddi.example.college.api

import arrow.core.Either
import dev.oblac.eddi.*
import dev.oblac.eddi.example.college.*
import dev.oblac.eddi.json.Json
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** Body of a successful command: the id of the student/course it concerned and the stored event's position. */
data class CommandResponse(val id: UUID, val seq: Seq)

/**
 * Runs [command] to completion off the engine's call threads (blocking JDBC inside) and returns
 * the outcome: the stored event, or the reason it was refused.
 */
suspend fun execute(command: Command): Either<CommandError, EventEnvelope<Event>> =
    withContext(Dispatchers.IO) { Main.execute(command) }

/**
 * Answers a command's outcome: [created] (201) or 200 with a [CommandResponse] built from the
 * emitted event via [idOf], or the mapped error status with an [ErrorResponse].
 */
suspend inline fun <reified E : Event> ApplicationCall.respondCommand(
    result: Either<CommandError, EventEnvelope<Event>>,
    created: Boolean = false,
    idOf: (E) -> UUID
) {
    result.fold(
        ifLeft = { error -> respondError(error.httpStatus(), error.toErrorResponse()) },
        ifRight = { stored ->
            val body = CommandResponse(idOf(stored.event as E), stored.sequence)
            respondText(Json.toJson(body), ContentType.Application.Json, if (created) HttpStatusCode.Created else HttpStatusCode.OK)
        }
    )
}

/**
 * HTTP status for a refused command: not-found guards → 404, state conflicts (already exists,
 * already paid, not paid, already enrolled, concurrent modification) → 409, invalid input → 422,
 * anything the API does not know → 500.
 */
fun CommandError.httpStatus(): HttpStatusCode = when (this) {
    is ConcurrencyConflict -> HttpStatusCode.Conflict
    is RegisterStudentError -> when (this) {
        RegisterStudentError.StudentAlreadyExist -> HttpStatusCode.Conflict
    }
    is UpdateStudentError -> when (this) {
        UpdateStudentError.StudentNotFound -> HttpStatusCode.NotFound
        UpdateStudentError.NothingToUpdate -> HttpStatusCode.UnprocessableEntity
    }
    is PayTuitionError -> when (this) {
        PayTuitionError.StudentNotFound -> HttpStatusCode.NotFound
        PayTuitionError.TuitionAlreadyPaid -> HttpStatusCode.Conflict
    }
    is PublishCourseError -> when (this) {
        PublishCourseError.CourseAlreadyExists -> HttpStatusCode.Conflict
    }
    is EnrollStudentInCourseError -> when (this) {
        is EnrollStudentInCourseError.StudentNotFound,
        is EnrollStudentInCourseError.CourseNotFound -> HttpStatusCode.NotFound
        is EnrollStudentInCourseError.TuitionNotPaid,
        is EnrollStudentInCourseError.AlreadyEnrolled -> HttpStatusCode.Conflict
    }
    else -> HttpStatusCode.InternalServerError
}

/** `{"error": "<ErrorType>", "message": "<human readable>"}` for a refused command. */
fun CommandError.toErrorResponse(): ErrorResponse = ErrorResponse(
    error = this::class.simpleName ?: "CommandError",
    message = when (this) {
        is ConcurrencyConflict -> "Concurrent modification, please retry"
        else -> toString()
    }
)
