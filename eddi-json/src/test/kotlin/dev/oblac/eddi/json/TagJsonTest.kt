package dev.oblac.eddi.json

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventName
import dev.oblac.eddi.Ref
import dev.oblac.eddi.Tag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@JvmInline
value class FixtureTag(override val id: UUID) : Tag<FixtureEvent>

data class FixtureEvent(
    val ownId: FixtureTag,
    val parent: FixtureTag?,
    val label: String,
    val at: Instant = Instant.EPOCH
) : Event

class TagJsonTest {

    @Test
    fun `Ref serializes as a uuid string keyed by event name`() {
        val uuid = UUID.fromString("3f2a0000-0000-0000-0000-000000000001")
        val ref = Ref(EventName("FixtureEvent"), uuid)

        assertEquals("""[{"FixtureEvent":"$uuid"}]""", Json.toJson(arrayOf(ref)))
    }

    @Test
    fun `Ref round-trips through JSON`() {
        val ref = Ref(EventName("FixtureEvent"), UUID.randomUUID())

        val back = Json.fromJson<Array<Ref>>(Json.toJson(arrayOf(ref)))

        assertEquals(ref, back.single())
    }

    @Test
    fun `event with a non-null and a non-null nullable tag round-trips`() {
        val event = FixtureEvent(FixtureTag(UUID.randomUUID()), FixtureTag(UUID.randomUUID()), "hello")

        val back = Json.fromNode(Json.valueToNode(event), FixtureEvent::class.java)

        assertEquals(event, back)
    }

    @Test
    fun `event with a null tag property round-trips`() {
        val event = FixtureEvent(FixtureTag(UUID.randomUUID()), null, "hello")

        val back = Json.fromNode(Json.valueToNode(event), FixtureEvent::class.java)

        assertEquals(event, back)
    }
}
