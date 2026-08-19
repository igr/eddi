package dev.oblac.eddi.json

import dev.oblac.eddi.Event
import dev.oblac.eddi.Id
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@JvmInline
value class FixtureId(override val id: UUID) : Id

data class FixtureEvent(
    val ownId: FixtureId,
    val parent: FixtureId?,
    val label: String,
    val at: Instant = Instant.EPOCH
) : Event {
    override fun ids() = listOfNotNull(ownId, parent)
}

class IdJsonTest {

    private val uuid = UUID.fromString("3f2a0000-0000-0000-0000-000000000001")

    @Test
    fun `ids encode as uuid strings keyed by id type name`() {
        val id: Id = FixtureId(uuid)

        assertEquals("""[{"FixtureId":"$uuid"}]""", Json.idsToNode(listOf(id)).toString())
    }

    @Test
    fun `an event's ids encode the same way`() {
        val event = FixtureEvent(FixtureId(uuid), null, "hello")

        assertEquals("""[{"FixtureId":"$uuid"}]""", Json.idsToNode(event.ids()).toString())
    }

    @Test
    fun `id properties in the payload stay plain uuid strings`() {
        val node = Json.valueToNode(FixtureEvent(FixtureId(uuid), FixtureId(uuid), "hello"))

        assertTrue(node["ownId"].isTextual, node.toString())
        assertEquals(uuid.toString(), node["ownId"].asText())
        assertEquals(uuid.toString(), node["parent"].asText())
    }

    @Test
    fun `ids() is not part of the serialized event`() {
        val event = FixtureEvent(FixtureId(UUID.randomUUID()), null, "hello")

        assertEquals(setOf("ownId", "parent", "label", "at"), Json.valueToNode(event).fieldNames().asSequence().toSet())
    }

    @Test
    fun `event with a non-null and a non-null nullable id round-trips`() {
        val event = FixtureEvent(FixtureId(UUID.randomUUID()), FixtureId(UUID.randomUUID()), "hello")

        val back = Json.fromNode(Json.valueToNode(event), FixtureEvent::class.java)

        assertEquals(event, back)
    }

    @Test
    fun `event with a null id property round-trips`() {
        val event = FixtureEvent(FixtureId(UUID.randomUUID()), null, "hello")

        val back = Json.fromNode(Json.valueToNode(event), FixtureEvent::class.java)

        assertEquals(event, back)
    }
}
