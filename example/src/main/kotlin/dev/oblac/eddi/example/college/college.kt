package dev.oblac.eddi.example.college

import dev.oblac.eddi.*
import dev.oblac.eddi.db.Db
import dev.oblac.eddi.db.DbEventStore


fun main() {
    val db = Db(
        jdbcUrl = "jdbc:postgresql://localhost:7432/eddi",
        username = "eddi_user",
        password = "eddi_password"
    )

    val eventStore = DbEventStore()
    val eventStoreInbox = eventStore as EventStoreInbox

    var coursePublishedId: ULong? = null
    var tuitionPaidId: ULong? = null

    val commandHandler: CommandHandler = { cmd ->
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

    val runCommand = runCommand(commandHandler)

    val eventHandler: EventListener = {
        when (val event = it.event as AppEvent) {
            is StudentRegistered -> studentRegistered(event)
            is CoursePublished -> coursePublished(event)
            is TuitionPaid -> tuitionPaid(event)
            else -> {}
        }
    }

    // real logic
    val eventListener: EventListener = {
        println("📢 Event received: ${it.event}")
        when (val event = it.event as AppEvent) {
            is StudentRegistered -> {
                runCommand(
                    PayTuition(
                        (it as EventEnvelope<StudentRegistered>).ref(),
                        1500.0,
                        "Fall 2024"
                    )
                )
            }

            is CoursePublished -> {
                val tuitionPaid = eventStore.findLastEventByTagBefore(it, TuitionPaidRef(tuitionPaidId?: 0uL).ref())
                if (tuitionPaid != null) {
                    println("TTTTTTTTTTTTTTTTTTTTTTTTTT")
                    runCommand(EnrollInCourse(
                        (tuitionPaid as EventEnvelope<TuitionPaid>).ref(),
                        CoursePublishedRef(it.sequence))
                    )
                }
            }

            is TuitionPaid -> {
                val coursePublished =
                    eventStore.findLastEventByTagBefore(it, CoursePublishedRef(coursePublishedId ?: 0uL).ref())
                if (coursePublished != null) {
                    println("CCCCCCCCCCCCCCCCCCCCCCCCCC")
                    runCommand(EnrollInCourse(
                        TuitionPaidRef(it.sequence),
                        (coursePublished as EventEnvelope<CoursePublished>).ref())
                    )
                }
            }

            is Enrolled -> {
                runCommand(GradeStudent((it as EventEnvelope<Enrolled>).ref(), "A"))
            }

            is Graded -> {
                println("✅ Student graded: ${event} -> ${event.grade}")
            }
        }
    }

    val dispatchEvent = dispatchEvent(eventHandler)
    val dispatcher = dispatchEvent + eventListener + Projections    // todo projections ARE INDEPENDENT!!!!!!!!!!!
    eventStore.startInbox { dispatcher(it) }

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