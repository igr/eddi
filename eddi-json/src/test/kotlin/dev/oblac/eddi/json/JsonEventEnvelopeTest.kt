package dev.oblac.eddi.json

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventName
import io.github.serpro69.kfaker.Faker
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class JsonEventEnvelopeTest {
    val faker = Faker()

    @Test
    fun `should serialize and deserialize EventEnvelope`() {
        // Given
        val event = TestEvent(faker.random.randomString(), faker.random.nextInt())
        val envelope = EventEnvelope(
            sequence = faker.random.nextLong().toULong(),
            correlationId = faker.random.nextLong().toULong(),
            event = event,
            eventName = EventName.of(event),
            timestamp = Instant.now()
        )

        // When
        val json = Json.toJson(envelope)
        val deserialized = Json.fromJson<EventEnvelope<TestEvent>>(json)

        // Then
        assertEquals(envelope.sequence, deserialized.sequence)
        assertEquals(envelope.correlationId, deserialized.correlationId)
        assertEquals(envelope.event, deserialized.event)
        assertEquals(envelope.eventName, deserialized.eventName)
        assertEquals(envelope.timestamp, deserialized.timestamp)
    }

    @Test
    fun `should serialize EventEnvelope and deserialize with generic Event type`() {
        // Given
        val event = TestEvent(faker.random.randomString(), faker.random.nextInt())
        val envelope = EventEnvelope.of(
            sequence = faker.random.nextLong().toULong(),
            correlationId = faker.random.nextLong().toULong(),
            event = event
        )

        // When
        val json = Json.toJson(envelope)
        val deserialized = Json.fromJson<EventEnvelope<Event>>(json)

        // Then
        assertEquals(envelope.sequence, deserialized.sequence)
        assertEquals(envelope.correlationId, deserialized.correlationId)

        val deserializedEvent = deserialized.event as TestEvent
        assertEquals(event.message, deserializedEvent.message)
        assertEquals(event.priority, deserializedEvent.priority)
    }
}
