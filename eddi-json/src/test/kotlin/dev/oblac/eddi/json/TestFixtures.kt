package dev.oblac.eddi.json

import dev.oblac.eddi.Event
import dev.oblac.eddi.Tag

// Test Event implementations
data class TestEvent(val message: String, val priority: Int) : Event

// Test Tag implementations
data class UserId(override val id: String) : Tag
data class SessionId(override val id: String) : Tag
data class TenantId(override val id: String) : Tag
