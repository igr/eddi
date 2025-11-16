package dev.oblac.eddi.json

import dev.oblac.eddi.Tag
import io.github.serpro69.kfaker.Faker
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonTagTest {
    val faker = Faker()

    @Test
    fun `should serialize and deserialize Tag`() {
        // Given
        val tag = UserId(faker.random.nextUUID())

        // When
        val json = Json.toTagJson(tag)
        val deserialized = Json.fromJson<UserId>(json)

        // Then
        assertEquals(tag, deserialized)
        assertEquals(tag.id, deserialized.id)
    }

    @Test
    fun `should serialize specific tag and deserialize as generic Tag`() {
        // Given
        val sessionId = SessionId(faker.random.nextUUID())

        // When
        val json = Json.toTagJson(sessionId)
        val deserialized = Json.fromJson<Tag>(json) as SessionId

        // Then
        assertEquals(sessionId, deserialized)
        assertEquals(sessionId.id, deserialized.id)
    }

    @Test
    fun `should include type information in Tag JSON`() {
        // Given
        val tenantId = TenantId(faker.random.nextUUID())

        // When
        val json = Json.toTagJson(tenantId)

        // Then
        assert(json.contains("TenantId")) {
            "JSON should contain type information: $json"
        }
    }
}
