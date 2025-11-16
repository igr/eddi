package dev.oblac.eddi.json

import dev.oblac.eddi.Tag
import io.github.serpro69.kfaker.Faker
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonTagArrayTest {
    val faker = Faker()

    @Test
    fun `should serialize and deserialize empty Tag array`() {
        // Given
        val tags = emptyArray<Tag>()

        // When
        val json = Json.toTagJson(tags)
        val deserialized = Json.fromJson<Array<Tag>>(json)

        // Then
        assertEquals(0, deserialized.size)
    }

    @Test
    fun `should serialize and deserialize Tag array with multiple different types`() {
        // Given
        val userId = UserId(faker.random.nextUUID())
        val sessionId = SessionId(faker.random.nextUUID())
        val tenantId = TenantId(faker.random.nextUUID())
        val tags = arrayOf(userId, sessionId, tenantId)

        // When
        val json = Json.toTagJson(tags)
        val deserialized = Json.fromJson<Array<Tag>>(json)

        // Then
        assertEquals(3, deserialized.size)

        val deserializedUserId = deserialized.find { it is UserId } as UserId
        val deserializedSessionId = deserialized.find { it is SessionId } as SessionId
        val deserializedTenantId = deserialized.find { it is TenantId } as TenantId

        assertEquals(userId.id, deserializedUserId.id)
        assertEquals(sessionId.id, deserializedSessionId.id)
        assertEquals(tenantId.id, deserializedTenantId.id)
    }

    @Test
    fun `should include type information for each Tag in array`() {
        // Given
        val userId = UserId(faker.random.nextUUID())
        val sessionId = SessionId(faker.random.nextUUID())
        val tags = arrayOf(userId, sessionId)

        // When
        val json = Json.toTagJson(tags)

        // Then
        assertTrue(json.contains("UserId"), "JSON should contain UserId type: $json")
        assertTrue(json.contains("SessionId"), "JSON should contain SessionId type: $json")
    }

    @Test
    fun `should preserve order of tags in array`() {
        // Given
        val tag1 = UserId("user-1")
        val tag2 = SessionId("session-1")
        val tag3 = TenantId("tenant-1")
        val tags = arrayOf(tag1, tag2, tag3)

        // When
        val json = Json.toTagJson(tags)
        val deserialized = Json.fromJson<Array<Tag>>(json)

        // Then
        assertEquals(3, deserialized.size)
        assertTrue(deserialized[0] is UserId)
        assertTrue(deserialized[1] is SessionId)
        assertTrue(deserialized[2] is TenantId)

        assertEquals("user-1", (deserialized[0] as UserId).id)
        assertEquals("session-1", (deserialized[1] as SessionId).id)
        assertEquals("tenant-1", (deserialized[2] as TenantId).id)
    }
}
