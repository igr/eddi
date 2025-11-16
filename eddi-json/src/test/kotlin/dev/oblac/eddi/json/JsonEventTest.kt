package dev.oblac.eddi.json

import dev.oblac.eddi.Event
import io.github.serpro69.kfaker.Faker
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonEventTest {
    val faker = Faker()

    @Test
    fun `should serialize and deserialize simple event`() {
        // Given
        val event = TestEvent(faker.random.randomString(), faker.random.nextInt())

        // When
        val json = Json.toEventJson(event)
        val deserialized = Json.fromJson<TestEvent>(json)

        // Then
        assertEquals(event, deserialized)
        assertEquals(event.message, deserialized.message)
        assertEquals(event.priority, deserialized.priority)
    }

    @Test
    fun `should serialize specific event and deserialize generic event`() {
        // Given
        val event = TestEvent(faker.random.randomString(), faker.random.nextInt())

        // When
        val json = Json.toEventJson(event)
        val deserialized = Json.fromJson<Event>(json) as TestEvent

        // Then
        assertEquals(event, deserialized)
        assertEquals(event.message, deserialized.message)
        assertEquals(event.priority, deserialized.priority)
    }
}
