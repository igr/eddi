package dev.oblac.eddi.example.college

import dev.oblac.eddi.*
import dev.oblac.eddi.example.createMemoryPlusSqlite
import java.util.*

fun main() {
    //val eddi = createMemoryEddie()
    val eddi = createMemoryPlusSqlite()

    with(eddi.serviceRegistry) {
        registerService(::registerStudent)
        registerService(::payTuition)
        registerService(EnrollInCourseService(eddi.eventStoreRepo))
        registerService(::publishCourse)
        registerService(::gradeStudent)
        registerService(::deregisterStudent)
    }

    createProjections(eddi)

    // USAGE

    eddi.eventBus.registerEventHandler(StudentRegistered::class) { ee: EventEnvelope<StudentRegistered> ->
        val studentRegistered = ee.event.id
        eddi.commandStore.storeCommand(PayTuition(studentRegistered, 1500.0, "Fall 2025"))
        emptyArray()
    }

    var coursePublishedId = CoursePublishedId("COURSE-UNKNOWN")
    eddi.eventBus.registerEventHandler(CoursePublished::class) { ee: EventEnvelope<CoursePublished> ->
        // we only have one course in this simple example
        coursePublishedId = ee.event.id
        emptyArray()
    }

    eddi.eventBus.registerEventHandler(TuitionPaid::class) { ee: EventEnvelope<TuitionPaid> ->
        val tuitionPaid = ee.event.id
        // find the course we published earlier
        // ⚠️ ISSUE: there is NO guarantee that the course was published before the tuition was paid!!
        val ee = eddi.eventStoreRepo.findLastTaggedEvent(Event.type<CoursePublished>(), coursePublishedId)
            ?: throw IllegalStateException("Course $coursePublishedId is not published")
        val coursePublished = ee.event as CoursePublished   // TODO remove cast

        eddi.commandStore.storeCommand(EnrollInCourse(tuitionPaid, coursePublished.id))
        emptyArray()
    }

    eddi.eventBus.registerEventHandler(Enrolled::class) { ee: EventEnvelope<Enrolled> ->
        val enrolled = ee.event.id
        eddi.commandStore.storeCommand(GradeStudent(enrolled, "A"))
        emptyArray()
    }

    val total = 1//_000_0000
    repeat(total) {
        eddi.commandStore.storeCommand(RegisterStudent("John${it + 1}", "Doe", "john.doe@college.edu"))
        eddi.commandStore.storeCommand(PublishCourse("Introduction to Computer Science", "Dr. Smith", 3))
    }

    println("College system initialized with sample commands")
    readln()
}

fun registerStudent(command: RegisterStudent): Array<StudentRegistered> {
    println("${System.currentTimeMillis()} 🔥 Registering student: ${command.firstName} ${command.lastName}")
    return arrayOf(
        StudentRegistered(
            id = StudentRegisteredId("STU-${UUID.randomUUID()}"),
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email
        )
    )
}

fun publishCourse(command: PublishCourse): Array<CoursePublished> {
    println("${System.currentTimeMillis()} 🔥 Publishing course: ${command.courseName}")
    return arrayOf(
        CoursePublished(
            id = CoursePublishedId("COURSE-${UUID.randomUUID()}"),
            courseName = command.courseName,
            instructor = command.instructor,
            credits = command.credits
        )
    )
}


fun payTuition(command: PayTuition): Array<TuitionPaid> {
    println("${System.currentTimeMillis()} 🔥 Processing tuition payment for student: ${command.student}")
    return arrayOf(
        TuitionPaid(
            id = TuitionPaidId("PAY-${UUID.randomUUID()}"),
            student = command.student,
            amount = command.amount,
            semester = command.semester
        )
    )
}

class EnrollInCourseService(val eventStore: EventStoreRepo) : Service<EnrollInCourse, Enrolled> {
    override fun invoke(command: EnrollInCourse): Array<Enrolled> {
        val tuitionPaid = command.tuitionPaid
        val course = command.course

        println("${System.currentTimeMillis()} 🔥 Enrolling student $tuitionPaid in course $course")

        return arrayOf(
            Enrolled(
                id = EnrolledId("ENR-${UUID.randomUUID()}"),
                tuitionPaid = tuitionPaid,
                course = course
            )
        )
    }
}

fun gradeStudent(command: GradeStudent): Array<Graded> {
    println("${System.currentTimeMillis()} 🔥 Grading ${command.grade}")
    return arrayOf(
        Graded(
            id = GradedId("GRD-${UUID.randomUUID()}"),
            enrolledId = command.enrolled,
            grade = command.grade
        )
    )
}

fun deregisterStudent(command: DeregisterStudent): Array<StudentDeregistered> {
    println("${System.currentTimeMillis()} 🔥 Processing student quit: ${command.student}")
    return arrayOf(
        StudentDeregistered(
            id = StudentDeregisteredId("STU-${UUID.randomUUID()}"),
            student = command.student,
            reason = command.reason
        )
    )
}