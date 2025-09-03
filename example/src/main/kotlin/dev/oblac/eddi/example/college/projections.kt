package dev.oblac.eddi.example.college

import dev.oblac.eddi.Eddi
import java.time.Instant

// Projection data classes
data class StudentProfile(
    val student: StudentTag,
    var firstName: String,
    var lastName: String,
    var email: String,
    var registeredAt: Instant,
    var deregisteredAt: Instant? = null,
    var deregistrationReason: String? = null
)

data class CourseInfo(
    val course: CourseTag,
    var courseName: String,
    var instructor: String,
    var credits: Int,
    var publishAt: Instant
)

data class EnrollmentRecord(
    val course: CourseTag,
    val enrolledAt: Instant
)

data class FinancialAccount(
    val student: StudentTag,
    var totalPaid: Double = 0.0,
    val payments: MutableList<PaymentRecord> = mutableListOf()
)

data class PaymentRecord(
    val amount: Double,
    val paidAt: Instant,
    val semester: String
)

data class TranscriptEntry(
    val course: CourseTag,
    val grade: String,
    val gradedAt: Instant
)


fun createProjections(eddi: Eddi) {

    // In-memory data stores for projections
    val studentProfiles = mutableMapOf<StudentTag, StudentProfile>()
    val courseCatalog = mutableMapOf<CourseTag, CourseInfo>()
    val enrollmentRecords = mutableMapOf<StudentTag, MutableList<EnrollmentRecord>>()
    val financialAccounts = mutableMapOf<StudentTag, FinancialAccount>()
    val academicTranscripts = mutableMapOf<StudentTag, MutableList<TranscriptEntry>>()

    // Projectors
    eddi.projector.projectorForEvent(StudentRegistered::class) { event ->
        studentProfiles[event.student] = StudentProfile(
            student = event.student,
            firstName = event.firstName,
            lastName = event.lastName,
            email = event.email,
            registeredAt = event.registeredAt
        )
    }

    eddi.projector.projectorForEvent(CoursePublished::class) { event ->
        courseCatalog[event.course] = CourseInfo(
            course = event.course,
            courseName = event.courseName,
            instructor = event.instructor,
            credits = event.credits,
            publishAt = event.publishAt
        )
    }
    eddi.projector.projectorForEvent(Enrolled::class) { event ->
        val records = enrollmentRecords.getOrPut(event.student) { mutableListOf() }
        records.add(EnrollmentRecord(course = event.course, enrolledAt = event.enrolledAt))
    }
    eddi.projector.projectorForEvent(TuitionPaid::class) { event ->
        val account = financialAccounts.getOrPut(event.student) { FinancialAccount(student = event.student) }
        account.totalPaid += event.amount
        account.payments.add(PaymentRecord(amount = event.amount, paidAt = event.paidAt, semester = event.semester))
    }
    eddi.projector.projectorForEvent(Graded::class) { event ->
        val transcript = academicTranscripts.getOrPut(event.student) { mutableListOf() }
        transcript.add(TranscriptEntry(course = event.course, grade = event.grade, gradedAt = event.gradedAt))
    }
    eddi.projector.projectorForEvent(StudentDeregistered::class) { event ->
        studentProfiles[event.student]?.let { profile ->
            profile.deregisteredAt = event.deregisteredAt
            profile.deregistrationReason = event.reason
        }
    }
}