package dev.oblac.eddi.db

import dev.oblac.eddi.Event
import dev.oblac.eddi.Id
import java.util.UUID

// Public (not private) on purpose: the events are serialized with Jackson and read back by class name.

@JvmInline
value class PingId(override val id: UUID) : Id

data class Pinged(val ping: PingId) : Event {
    override fun ids() = listOf(ping)
}

data class Ponged(val ping: PingId, val other: PingId) : Event {
    override fun ids() = listOf(ping, other)
}
