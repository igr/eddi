package dev.oblac.eddi.example.college

import dev.oblac.eddi.Eddi
import java.time.Instant


// Projection data classes
data class StudentProfile(
    val studentId: String,
    var firstName: String,
    var lastName: String,
    var email: String,
    var registeredAt: Instant,
    var deregisteredAt: Instant? = null,
    var deregistrationReason: String? = null
)

data class CourseInfo(
    val courseId: String,
    var courseName: String,
    var instructor: String,
    var credits: Int,
    var publishAt: Instant
)

data class EnrollmentRecord(
    val courseId: String,
    val enrolledAt: Instant
)

data class FinancialAccount(
    val studentId: String,
    var totalPaid: Double = 0.0,
    val payments: MutableList<PaymentRecord> = mutableListOf()
)

data class PaymentRecord(
    val amount: Double,
    val paidAt: Instant,
    val semester: String
)

data class TranscriptEntry(
    val courseId: String,
    val grade: String,
    val gradedAt: Instant
)


fun projections(eddi: Eddi) {

    // In-memory data stores for projections
    val studentProfiles = mutableMapOf<String, StudentProfile>()
    val courseCatalog = mutableMapOf<String, CourseInfo>()
    val enrollmentRecords = mutableMapOf<String, MutableList<EnrollmentRecord>>()
    val financialAccounts = mutableMapOf<String, FinancialAccount>()
    val academicTranscripts = mutableMapOf<String, MutableList<TranscriptEntry>>()

    // Projectors
    eddi.projector.projectorForEvent(StudentRegistered::class) { event ->
        studentProfiles[event.studentId] = StudentProfile(
            studentId = event.studentId,
            firstName = event.firstName,
            lastName = event.lastName,
            email = event.email,
            registeredAt = event.registeredAt
        )
        println("Projected StudentRegistered to StudentProfile: ${studentProfiles[event.studentId]}")
    }

    eddi.projector.projectorForEvent(CoursePublished::class) { event ->
        courseCatalog[event.courseId] = CourseInfo(
            courseId = event.courseId,
            courseName = event.courseName,
            instructor = event.instructor,
            credits = event.credits,
            publishAt = event.publishAt
        )
        println("Projected CoursePublished to CourseInfo: ${courseCatalog[event.courseId]}")
    }
    eddi.projector.projectorForEvent(Enrolled::class) { event ->
        val records = enrollmentRecords.getOrPut(event.studentId) { mutableListOf() }
        records.add(EnrollmentRecord(courseId = event.courseId, enrolledAt = event.enrolledAt))
        println("Projected Enrolled to EnrollmentRecord: ${records.last()}")
    }
    eddi.projector.projectorForEvent(TuitionPaid::class) { event ->
        val account = financialAccounts.getOrPut(event.studentId) { FinancialAccount(studentId = event.studentId) }
        account.totalPaid += event.amount
        account.payments.add(PaymentRecord(amount = event.amount, paidAt = event.paidAt, semester = event.semester))
        println("Projected TuitionPaid to FinancialAccount: ${account}")
    }
    eddi.projector.projectorForEvent(Graded::class) { event ->
        val transcript = academicTranscripts.getOrPut(event.studentId) { mutableListOf() }
        transcript.add(TranscriptEntry(courseId = event.courseId, grade = event.grade, gradedAt = event.gradedAt))
        println("Projected Graded to TranscriptEntry: ${transcript.last()}")
    }
    eddi.projector.projectorForEvent(StudentDeregistered::class) { event ->
        studentProfiles[event.studentId]?.let { profile ->
            profile.deregisteredAt = event.deregisteredAt
            profile.deregistrationReason = event.reason
            println("Projected StudentDeregistered to StudentProfile: $profile")
        }
    }
}