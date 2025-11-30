package dev.oblac.eddi.example.college

import dev.oblac.eddi.*
import dev.oblac.eddi.db.DbEventStore

fun main() {
    val es = DbEventStore()
    val esInbox = es as EventStoreInbox

    var coursePublishedId: Seq? = null
    var tuitionPaidId: Seq? = null

    val runCommand = commandHandler { cmd ->
        when (val command = cmd as AppCommand) {
            is RegisterStudent -> registerStudent(esInbox, command)
            is PublishCourse -> publishCourse(esInbox, command) {
                coursePublishedId = it
            }
            is PayTuition -> payTuition(esInbox, command) {
                tuitionPaidId = it
            }
            is EnrollInCourse -> enrollInCourse(esInbox, command)
            is GradeStudent -> gradeStudent(esInbox, command)
        }
    }

    val eventHandler = EventListener { ee ->
        ee.invoke<StudentRegistered> { studentRegistered(it.event) }
        ee.invoke<CoursePublished> { coursePublished(it.event) }
        ee.invoke<TuitionPaid> { tuitionPaid(it.event) }
    }

    val eventListener = EventListener { ee ->
        println("📢 Event received: ${ee.event}")
        ee.invoke<StudentRegistered> {
            runCommand<Unit>(
                PayTuition(it.tag(), 1500.0, "Fall 2024")
            )
        }
        ee.invoke<CoursePublished> {
            val tuitionPaid = es.findLastEventByTagBefore(it.sequence, TuitionPaidTag(tuitionPaidId ?: Seq.ZERO))
            if (tuitionPaid != null) {
                runCommand<Unit>(
                    EnrollInCourse(tuitionPaid.tag(), CoursePublishedTag(ee.sequence))
                )
            }
        }
        ee.invoke<TuitionPaid> {
            val coursePublished = es.findLastEventByTagBefore(it.sequence, CoursePublishedTag(coursePublishedId ?: Seq.ZERO))
            if (coursePublished != null) {
                runCommand<Unit>(
                    EnrollInCourse(TuitionPaidTag(it.sequence), coursePublished.tag())
                )
            }
        }
        ee.invoke<Enrolled> {
            runCommand<Unit>(GradeStudent(it.tag(), "A"))
        }
        ee.invoke<Graded> {
            println("✅ Student graded: ${it.event} -> ${it.event.grade}")
        }
    }

    val dispatcher = eventHandler + eventListener
    es.startInbox { dispatcher(it) }

    Projections.start()

    /* RUN */
    runCommand<Unit>(RegisterStudent("John", "Doe", "john@foo.com"))
    runCommand<Unit>(PublishCourse("Intro to Programming", "Dr. Smith", 3))
    readln()
}

fun registerStudent(inbox: EventStoreInbox, command: RegisterStudent): arrow.core.Either<CommandError, Unit> {
    println("${System.currentTimeMillis()} 🔥 Registering student: ${command.firstName} ${command.lastName}")
    inbox.storeEvent(
        StudentRegistered(
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email
        )
    )
    return arrow.core.Either.Right(Unit)
}

fun studentRegistered(event: StudentRegistered) {
    println("${System.currentTimeMillis()} 🎉 Student registered with ID: $event")
}

fun publishCourse(inbox: EventStoreInbox, command: PublishCourse, consumeId: (Seq) -> Unit): arrow.core.Either<CommandError, Unit> {
    println("${System.currentTimeMillis()} 🔥 Publishing course: ${command.courseName}")
    val e = inbox.storeEvent(
        CoursePublished(
            courseName = command.courseName,
            instructor = command.instructor,
            credits = command.credits
        )
    )
    consumeId(e.sequence)
    return arrow.core.Either.Right(Unit)
}

fun coursePublished(event: CoursePublished) {
    println("${System.currentTimeMillis()} 🎉 Course published with ID: $event")
}

fun payTuition(inbox: EventStoreInbox, command: PayTuition, consumeId: (Seq) -> Unit): arrow.core.Either<CommandError, Unit> {
    println("${System.currentTimeMillis()} 🔥 Processing tuition payment for student: ${command.student}")
    val e = inbox.storeEvent(
        TuitionPaid(
            student = command.student,
            amount = command.amount,
            semester = command.semester
        )
    )
    consumeId(e.sequence)
    return arrow.core.Either.Right(Unit)
}

fun tuitionPaid(event: TuitionPaid) {
    println("${System.currentTimeMillis()} 🎉 Tuition paid with ID: $event for amount: ${event.amount}")
}

fun enrollInCourse(inbox: EventStoreInbox, command: EnrollInCourse): arrow.core.Either<CommandError, Unit> {
    println("${System.currentTimeMillis()} 🔥 Enrolling student ${command.tuitionPaid} in course ${command.course}")
    inbox.storeEvent(
        Enrolled(
            tuitionPaid = command.tuitionPaid,
            course = command.course
        )
    )
    return arrow.core.Either.Right(Unit)
}

fun gradeStudent(inbox: EventStoreInbox, command: GradeStudent): arrow.core.Either<CommandError, Unit> {
    println("${System.currentTimeMillis()} 🔥 Grading ${command.grade}")
    inbox.storeEvent(
        Graded(
            enrolled = command.enrolled,
            grade = command.grade
        )
    )
    return arrow.core.Either.Right(Unit)
}

//fun deregisterStudent(command: DeregisterStudent): Array<StudentDeregistered> {
//    println("${System.currentTimeMillis()} 🔥 Processing student quit: ${command.student}")
//    return arrayOf(
//        StudentDeregistered(
//            id = StudentDeregisteredId("STU-${UUID.randomUUID()}"),
//            student = command.student,
//            reason = command.reason
//        )
//    )
//}