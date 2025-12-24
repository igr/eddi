package dev.oblac.eddi.example.college

import arrow.core.Either
import dev.oblac.eddi.EventStore
import dev.oblac.eddi.UnknownCommandError
import dev.oblac.eddi.commandHandler
import dev.oblac.eddi.example.college.cmd.*

/**
 * Main command handler that routes commands to their respective handlers.
 */
fun commandHandler(es: EventStore) = commandHandler { command ->
    when (command) {
        is RegisterStudent -> registerStudent(es, command).map {
            es.storeEvent(it)
        }
        is UpdateStudent -> updateStudent(es, command).map {
            es.storeEvent(it)
        }

        is PayTuition -> payStudentTuition(
            command,
            studentExists = { student ->
                es.findEvent<StudentRegistered>(
                    student.seq,
                    StudentRegisteredEvent.NAME,
                ) != null
            },
            studentNotAlreadyPayed = { student ->
                es.findEventByTag(
                    TuitionPaidEvent.NAME,
                    StudentRegisteredTag(student.seq)
                ) == null
            }).map {
                es.storeEvent(it)
            }

        is PublishCourse -> publishCourse(
            command,
            courseExists = { courseName ->
                es.findEvents<CoursePublished>(
                    CoursePublishedEvent.NAME,
                    mapOf("courseName" to courseName)
                ).isNotEmpty()
            }
        ).map {
            es.storeEvent(it)
        }

        is EnrollStudentInCourse -> enrollStudentInCourse(
            command,
            studentExists = { student ->
                es.findEvent<StudentRegistered>(
                    student.seq,
                    StudentRegisteredEvent.NAME,
                ) != null
            },
            courseExists = { course ->
                es.findEvent<CoursePublished>(
                    course.seq,
                    CoursePublishedEvent.NAME,
                ) != null
            },
            tuitionPaid = { student ->
                es.findEventByTag(
                    TuitionPaidEvent.NAME,
                    StudentRegisteredTag(student.seq)
                ) != null
            },
            unique = { student, course ->
                es.findEventByMultipleTags<StudentEnrolledInCourse>(
                    StudentEnrolledInCourseEvent.NAME,
                    StudentRegisteredTag(student.seq),
                    CoursePublishedTag(course.seq)
                ) == null
            }
        ).map {
            es.storeEvent(it)
        }

        else -> {
            println("Unknown command: $command")
            Either.Left(UnknownCommandError(command))
        }
    }
}

