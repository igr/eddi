package dev.oblac.eddi.example.college

import dev.oblac.eddi.*
import dev.oblac.eddi.EventListener
import dev.oblac.eddi.memory.MemEventStore
import java.util.*


fun main() {

    val eventStore = MemEventStore()
    val eventStoreInbox = eventStore as EventStoreInbox

    val commandHandler: CommandHandler = { cmd ->
        when (val command = cmd as AppCommand) {
            is RegisterStudent -> registerStudent(eventStoreInbox, command)
            is PublishCourse -> publishCourse(eventStoreInbox, command)
            is PayTuition -> payTuition(eventStoreInbox, command)
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
                runCommand(PayTuition(event.id, 1500.0, "Fall 2024"))
            }

            is CoursePublished -> {
                val tuitionPaid = eventStore.findLastTaggedEvent(EventType.of(TuitionPaid::class))
                if (tuitionPaid != null) {
                    runCommand(EnrollInCourse((tuitionPaid as EventEnvelope<TuitionPaid>).event.id, event.id))
                }
            }

            is TuitionPaid -> {
                val coursePublished = eventStore.findLastTaggedEvent(EventType.of(CoursePublished::class))
                if (coursePublished != null) {
                    runCommand(EnrollInCourse(event.id, (coursePublished as EventEnvelope<CoursePublished>).event.id))
                }
            }

            is Enrolled -> {
                runCommand(GradeStudent(event.id, "A"))
            }

            is Graded -> {
                println("✅ Student graded: ${event.enrolledId} -> ${event.grade}")
            }
        }
    }

    val dispatchEvent = dispatchEvent(eventHandler)
    val dispatcher = dispatchEvent + eventListener + Projections
    eventStore.startInbox { dispatcher(it) }

    runCommand(RegisterStudent("John", "Doe", "john@foo.com"))
    runCommand(PublishCourse("Intro to Programming", "Dr. Smith", 3))

    readln()
}

fun registerStudent(inbox: EventStoreInbox, command: RegisterStudent) {
    println("${System.currentTimeMillis()} 🔥 Registering student: ${command.firstName} ${command.lastName}")
    inbox.storeEvent(
        0, StudentRegistered(
            id = StudentRegisteredId("STU-${UUID.randomUUID()}"),
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email
        )
    )
}

fun studentRegistered(event: StudentRegistered) {
    println("${System.currentTimeMillis()} 🎉 Student registered with ID: ${event.id}")
}

fun publishCourse(inbox: EventStoreInbox, command: PublishCourse) {
    println("${System.currentTimeMillis()} 🔥 Publishing course: ${command.courseName}")
    inbox.storeEvent(0,
        CoursePublished(
            id = CoursePublishedId("COURSE-${UUID.randomUUID()}"),
            courseName = command.courseName,
            instructor = command.instructor,
            credits = command.credits
        )
    )
}

fun coursePublished(event: CoursePublished) {
    println("${System.currentTimeMillis()} 🎉 Course published with ID: ${event.id}")
}

fun payTuition(inbox: EventStoreInbox, command: PayTuition) {
    println("${System.currentTimeMillis()} 🔥 Processing tuition payment for student: ${command.student}")
    inbox.storeEvent(
        0,
        TuitionPaid(
            id = TuitionPaidId("PAY-${UUID.randomUUID()}"),
            student = command.student,
            amount = command.amount,
            semester = command.semester
        )
    )
}

fun tuitionPaid(event: TuitionPaid) {
    println("${System.currentTimeMillis()} 🎉 Tuition paid with ID: ${event.id} for amount: ${event.amount}")
}

fun enrollInCourse(inbox: EventStoreInbox, command: EnrollInCourse) {
    println("${System.currentTimeMillis()} 🔥 Enrolling student ${command.tuitionPaid} in course ${command.course}")
    inbox.storeEvent(
        0,
        Enrolled(
            id = EnrolledId("ENR-${UUID.randomUUID()}"),
            tuitionPaid = command.tuitionPaid,
            course = command.course
        )
    )
}

fun gradeStudent(inbox: EventStoreInbox, command: GradeStudent) {
    println("${System.currentTimeMillis()} 🔥 Grading ${command.grade}")
    inbox.storeEvent(0,
        Graded(
            id = GradedId("GRD-${UUID.randomUUID()}"),
            enrolledId = command.enrolled,
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