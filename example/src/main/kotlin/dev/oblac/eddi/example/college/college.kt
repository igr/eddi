package dev.oblac.eddi.example.college

import dev.oblac.eddi.*
import dev.oblac.eddi.db.Db
import dev.oblac.eddi.db.DbEventStore
import dev.oblac.eddi.meta.EventsRegistry

fun main() {
    // we need to register events before using the event store
    EventsRegistry.init()

    val db = Db(
        jdbcUrl = "jdbc:postgresql://localhost:7432/eddi",
        username = "eddi_user",
        password = "eddi_password"
    )

    val eventStore = DbEventStore()
    val eventStoreInbox = eventStore as EventStoreInbox

    var coursePublishedId: ULong? = null
    var tuitionPaidId: ULong? = null

    val runCommand: CommandHandler = { cmd ->
        when (val command = cmd as AppCommand) {
            is RegisterStudent -> registerStudent(eventStoreInbox, command)
            is PublishCourse -> publishCourse(eventStoreInbox, command) {
                coursePublishedId = it
            }
            is PayTuition -> payTuition(eventStoreInbox, command) {
                tuitionPaidId = it
            }
            is EnrollInCourse -> enrollInCourse(eventStoreInbox, command)
            is GradeStudent -> gradeStudent(eventStoreInbox, command)
        }
    }

    val eventHandler: EventListener = { ee ->
        ee.onEvent<StudentRegistered> { studentRegistered(it.event) }
        ee.onEvent<CoursePublished> { coursePublished(it.event) }
        ee.onEvent<TuitionPaid> { tuitionPaid(it.event) }
    }

    val eventListener: EventListener = { ee ->
        println("📢 Event received: ${ee.event}")
        ee.onEvent<StudentRegistered> {
            runCommand(
                PayTuition(it.tag(), 1500.0, "Fall 2024")
            )
        }
        ee.onEvent<CoursePublished> {
            val tuitionPaid = eventStore.findLastEventByTagBefore(it.sequence, TuitionPaidTag(tuitionPaidId?: 0uL))
            if (tuitionPaid != null) {
                runCommand(
                    EnrollInCourse(tuitionPaid.tag(), CoursePublishedTag(ee.sequence))
                )
            }
        }
        ee.onEvent<TuitionPaid> {
            val coursePublished = eventStore.findLastEventByTagBefore(it.sequence, CoursePublishedTag(coursePublishedId ?: 0u))
            if (coursePublished != null) {
                runCommand(
                    EnrollInCourse(TuitionPaidTag(it.sequence), coursePublished.tag())
                )
            }
        }
        ee.onEvent<Enrolled> {
            runCommand(GradeStudent(it.tag(), "A"))
        }
        ee.onEvent<Graded> {
            println("✅ Student graded: ${it.event} -> ${it.event.grade}")
        }
    }

    val dispatcher = eventHandler + eventListener
    eventStore.startInbox { dispatcher(it) }

    Projections.start()

    /* RUN */
    runCommand(RegisterStudent("John", "Doe", "john@foo.com"))
    runCommand(PublishCourse("Intro to Programming", "Dr. Smith", 3))
    readln()
}

fun registerStudent(inbox: EventStoreInbox, command: RegisterStudent) {
    println("${System.currentTimeMillis()} 🔥 Registering student: ${command.firstName} ${command.lastName}")
    inbox.storeEvent(
        0u, StudentRegistered(
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email
        )
    )
}

fun studentRegistered(event: StudentRegistered) {
    println("${System.currentTimeMillis()} 🎉 Student registered with ID: $event")
}

fun publishCourse(inbox: EventStoreInbox, command: PublishCourse, consumeId: (ULong) -> Unit) {
    println("${System.currentTimeMillis()} 🔥 Publishing course: ${command.courseName}")
    val e = inbox.storeEvent(
        0u,
        CoursePublished(
            courseName = command.courseName,
            instructor = command.instructor,
            credits = command.credits
        )
    )
    consumeId(e.sequence)
}

fun coursePublished(event: CoursePublished) {
    println("${System.currentTimeMillis()} 🎉 Course published with ID: $event")
}

fun payTuition(inbox: EventStoreInbox, command: PayTuition, consumeId: (ULong) -> Unit) {
    println("${System.currentTimeMillis()} 🔥 Processing tuition payment for student: ${command.student}")
    val e = inbox.storeEvent(
        0u,
        TuitionPaid(
            student = command.student,
            amount = command.amount,
            semester = command.semester
        )
    )
    consumeId(e.sequence)
}

fun tuitionPaid(event: TuitionPaid) {
    println("${System.currentTimeMillis()} 🎉 Tuition paid with ID: ${event} for amount: ${event.amount}")
}

fun enrollInCourse(inbox: EventStoreInbox, command: EnrollInCourse) {
    println("${System.currentTimeMillis()} 🔥 Enrolling student ${command.tuitionPaid} in course ${command.course}")
    inbox.storeEvent(
        0u,
        Enrolled(
            tuitionPaid = command.tuitionPaid,
            course = command.course
        )
    )
}

fun gradeStudent(inbox: EventStoreInbox, command: GradeStudent) {
    println("${System.currentTimeMillis()} 🔥 Grading ${command.grade}")
    inbox.storeEvent(
        0u,
        Graded(
            enrolled = command.enrolled,
            grade = command.grade
        )
    )
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