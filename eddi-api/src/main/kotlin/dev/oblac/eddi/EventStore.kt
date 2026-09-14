package dev.oblac.eddi

interface EventStore : EventStoreInbox, EventStoreRepo {

    /**
     * Version of [id]: the sequence of the latest event carrying it, [Seq.ZERO] if none.
     */
    fun versionOf(id: Id): Seq
}
