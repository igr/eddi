package dev.oblac.eddi.example.college

import dev.oblac.eddi.example.college.StubEventStoreRepo.Companion.envelope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class CommandProcessorTest {

    private val studentId = StudentId(UUID.randomUUID())
    private val courseId = CourseId(UUID.randomUUID())

    private fun registered() =
        envelope(StudentRegistered(studentId, "Ada", "Lovelace", "ada@college.edu"))

    @Test
    fun `RegisterStudent mints a distinct student id per registration`() {
        val cmd = RegisterStudent("Ada", "Lovelace", "ada@college.edu")

        val first = cmd(StubEventStoreRepo()).getOrNull()!!
        val second = cmd(StubEventStoreRepo()).getOrNull()!!

        assertNotEquals(first.studentId, second.studentId)
    }

    @Test
    fun `PublishCourse mints a distinct course id per publication`() {
        val cmd = PublishCourse("Algebra", "Noether")

        val first = cmd(StubEventStoreRepo()).getOrNull()!!
        val second = cmd(StubEventStoreRepo()).getOrNull()!!

        assertNotEquals(first.courseId, second.courseId)
    }

    @Test
    fun `StudentRegistered carries its own id`() {
        val event = StudentRegistered(studentId, "Ada", "Lovelace", "ada@college.edu")

        assertEquals(listOf(studentId), event.ids())
    }

    @Test
    fun `UpdateStudent emits an event carrying the student id`() {
        val repo = StubEventStoreRepo(listOf(registered()))

        val event = UpdateStudent(studentId, "Ada", null)(repo).getOrNull()!!

        assertEquals(studentId, event.student)
    }

    @Test
    fun `StudentUpdated carries the id of the student it updates`() {
        val event = StudentUpdated(studentId, "Grace", null)

        assertEquals(listOf(studentId), event.ids())
    }

    @Test
    fun `UpdateStudent fails when the student does not exist`() {
        val cmd = UpdateStudent(studentId, "Ada", null)

        val result = cmd(StubEventStoreRepo())

        assertTrue(result.isLeft())
    }

    @Test
    fun `PayTuition fails when the student does not exist`() {
        assertTrue(PayTuition(studentId)(StubEventStoreRepo()).isLeft())
    }

    @Test
    fun `StudentEnrolledInCourse carries both the student and the course ids`() {
        val event = StudentEnrolledInCourse(studentId, courseId)

        assertEquals(listOf(studentId, courseId), event.ids())
    }
}
