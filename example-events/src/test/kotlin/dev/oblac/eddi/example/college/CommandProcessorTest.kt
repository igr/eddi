package dev.oblac.eddi.example.college

import dev.oblac.eddi.Ref
import dev.oblac.eddi.example.college.StubEventStoreRepo.Companion.envelope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class CommandProcessorTest {

    private val studentId = StudentId(UUID.randomUUID())
    private val courseId = CoursePublishedTag(UUID.randomUUID())

    private fun registered() =
        envelope(
            StudentRegistered(studentId, "Ada", "Lovelace", "ada@college.edu"),
            StudentRegisteredEvent.NAME
        )

    @Test
    fun `RegisterStudent emits an event carrying the supplied id`() {
        val cmd = RegisterStudent(studentId, "Ada", "Lovelace", "ada@college.edu")

        val event = cmd(StubEventStoreRepo()).getOrNull()!!

        assertEquals(studentId, event.studentId)
    }

    @Test
    fun `StudentRegistered self-tags with its own id`() {
        val event = StudentRegistered(studentId, "Ada", "Lovelace", "ada@college.edu")

        val refs = StudentRegisteredEvent.refs(event).toList()

        assertEquals(listOf(Ref(StudentRegisteredEvent.NAME, studentId.id)), refs)
    }

    @Test
    fun `UpdateStudent derives a null last when there is no previous update`() {
        val updateId = StudentUpdatedTag(UUID.randomUUID())
        val repo = StubEventStoreRepo(listOf(registered()))

        val event = UpdateStudent(updateId, studentId, "Ada", null)(repo).getOrNull()!!

        assertEquals(updateId, event.updateId)
        assertNull(event.last)
    }

    @Test
    fun `UpdateStudent chains last to the previous update's id`() {
        val previousId = StudentUpdatedTag(UUID.randomUUID())
        val updateId = StudentUpdatedTag(UUID.randomUUID())
        val repo = StubEventStoreRepo(
            listOf(
                registered(),
                envelope(
                    StudentUpdated(previousId, studentId, null, "Ada", null),
                    StudentUpdatedEvent.NAME,
                    seq = 2L
                )
            )
        )

        val event = UpdateStudent(updateId, studentId, "Grace", null)(repo).getOrNull()!!

        assertEquals(previousId, event.last)
        assertEquals(
            listOf(
                Ref(StudentUpdatedEvent.NAME, updateId.id),
                Ref(StudentRegisteredEvent.NAME, studentId.id),
                Ref(StudentUpdatedEvent.NAME, previousId.id)
            ),
            StudentUpdatedEvent.refs(event).toList()
        )
    }

    @Test
    fun `UpdateStudent fails when the student does not exist`() {
        val cmd = UpdateStudent(StudentUpdatedTag(UUID.randomUUID()), studentId, "Ada", null)

        val result = cmd(StubEventStoreRepo())

        assertTrue(result.isLeft())
    }

    @Test
    fun `PayTuition fails when the student does not exist`() {
        assertTrue(PayTuition(studentId)(StubEventStoreRepo()).isLeft())
    }

    @Test
    fun `StudentEnrolledInCourse refs both the student and the course`() {
        val event = StudentEnrolledInCourse(studentId, courseId)

        assertEquals(
            listOf(
                Ref(StudentRegisteredEvent.NAME, studentId.id),
                Ref(CoursePublishedEvent.NAME, courseId.id)
            ),
            StudentEnrolledInCourseEvent.refs(event).toList()
        )
    }
}
