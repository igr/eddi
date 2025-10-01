package dev.oblac.eddi.example.college

import dev.oblac.eddi.Eddi
import dev.oblac.eddi.Event
import java.time.Instant

// Projection data classes
data class StudentProfile(
    val student: StudentRegisteredId,
    var firstName: String,
    var lastName: String,
    var email: String,
    var registeredAt: Instant,
    var deregisteredAt: Instant? = null,
    var deregistrationReason: String? = null
)

data class CourseInfo(
    val course: CoursePublishedId,
    var courseName: String,
    var instructor: String,
    var credits: Int,
    var publishAt: Instant
)

data class EnrollmentRecord(
    val course: EnrolledId,
    val enrolledAt: Instant
)

data class FinancialAccount(
    val student: StudentRegisteredId,
    var totalPaid: Double = 0.0,
    val payments: MutableList<PaymentRecord> = mutableListOf()
)

data class PaymentRecord(
    val amount: Double,
    val paidAt: Instant,
    val semester: String
)

data class TranscriptEntry(
    val course: CoursePublishedId,
    val grade: String,
    val gradedAt: Instant
)


fun createProjections(eddi: Eddi) {

    // In-memory data stores for projections
    val studentProfiles = mutableMapOf<StudentRegisteredId, StudentProfile>()
    val courseCatalog = mutableMapOf<CoursePublishedId, CourseInfo>()
    val enrollmentRecords = mutableMapOf<StudentRegisteredId, MutableList<EnrollmentRecord>>()
    val financialAccounts = mutableMapOf<StudentRegisteredId, FinancialAccount>()
    val academicTranscripts = mutableMapOf<StudentRegisteredId, MutableList<TranscriptEntry>>()

    // Projectors
    eddi.projector.projectorForEvent(StudentRegistered::class) { event ->
        studentProfiles[event.id] = StudentProfile(
            student = event.id,
            firstName = event.firstName,
            lastName = event.lastName,
            email = event.email,
            registeredAt = event.registeredAt
        )
    }

    eddi.projector.projectorForEvent(CoursePublished::class) { event ->
        courseCatalog[event.id] = CourseInfo(
            course = event.id,
            courseName = event.courseName,
            instructor = event.instructor,
            credits = event.credits,
            publishAt = event.publishAt
        )
    }
    eddi.projector.projectorForEvent(Enrolled::class) { event ->
        // Get the TuitionPaid event to find the student
        val tuitionPaidEnvelope = eddi.eventStoreRepo.findLastTaggedEvent(Event.type<TuitionPaid>(), event.tuitionPaid)
            ?: return@projectorForEvent
        val tuitionPaid = tuitionPaidEnvelope.event as TuitionPaid

        val records = enrollmentRecords.getOrPut(tuitionPaid.student) { mutableListOf() }
        records.add(EnrollmentRecord(course = event.id, enrolledAt = event.enrolledAt))
    }
    eddi.projector.projectorForEvent(TuitionPaid::class) { event ->
        val account = financialAccounts.getOrPut(event.student) { FinancialAccount(student = event.student) }
        account.totalPaid += event.amount
        account.payments.add(PaymentRecord(amount = event.amount, paidAt = event.paidAt, semester = event.semester))
    }
    eddi.projector.projectorForEvent(Graded::class) { event ->
        // Get the Enrolled event to find the course and student
        val enrolledEnvelope = eddi.eventStoreRepo.findLastTaggedEvent(Event.type<Enrolled>(), event.enrolledId)
            ?: return@projectorForEvent
        val enrolled = enrolledEnvelope.event as Enrolled

        // Get the TuitionPaid event to find the student
        val tuitionPaidEnvelope = eddi.eventStoreRepo.findLastTaggedEvent(Event.type<TuitionPaid>(), enrolled.tuitionPaid)
            ?: return@projectorForEvent
        val tuitionPaid = tuitionPaidEnvelope.event as TuitionPaid

        val transcript = academicTranscripts.getOrPut(tuitionPaid.student) { mutableListOf() }
        transcript.add(TranscriptEntry(course = enrolled.course, grade = event.grade, gradedAt = event.gradedAt))
    }
    eddi.projector.projectorForEvent(StudentDeregistered::class) { event ->
        studentProfiles[event.student]?.let { profile ->
            profile.deregisteredAt = event.deregisteredAt
            profile.deregistrationReason = event.reason
        }
    }
}