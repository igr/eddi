package dev.oblac.eddi.example.college

import dev.oblac.eddi.Event
import dev.oblac.eddi.EventEnvelope
import dev.oblac.eddi.EventListener
import dev.oblac.eddi.db.DbEventProcessor
import dev.oblac.eddi.example.college.projection.dbInsertStudent
import dev.oblac.eddi.example.college.projection.dbUpdateStudent
import dev.oblac.eddi.example.college.projection.dbUpdateTuitionPayment
import dev.oblac.eddi.on

object Projections : EventListener {
    private val dbEventProcessor = DbEventProcessor(100L)

    override fun invoke(envelope: EventEnvelope<Event>) {
        envelope.on<StudentRegistered> { dbInsertStudent(it) }
        envelope.on<StudentUpdated> { dbUpdateStudent(it) }
        envelope.on<TuitionPaid> { dbUpdateTuitionPayment(it) }
    }

    fun start() {
        dbEventProcessor.startInbox(this)
        println("🚀 Projections started")
    }

}