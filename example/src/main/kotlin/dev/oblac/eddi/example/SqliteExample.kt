package dev.oblac.eddi.example

import dev.oblac.eddi.Event
import dev.oblac.eddi.example.college.*
import dev.oblac.eddi.sqlite.SqliteEddiFactory
import kotlinx.coroutines.runBlocking

/**
 * Example demonstrating the use of SQLite-based Event Store.
 * 
 * This example shows how to:
 * 1. Set up a SQLite Event Store using the factory
 * 2. Store events persistently in SQLite database
 * 3. Query events using the EventStoreRepo interface
 * 4. Handle database statistics and monitoring
 */
fun main() = runBlocking {
    println("=== Eddi SQLite Event Store Example ===\n")
    
    // Create SQLite-based Event Store components
    val (eventStore, eventStoreRepo) = SqliteEddiFactory.createEventStore(
        databasePath = "example_event_store.db"
    )
    
    println("✅ SQLite Event Store initialized at: example_event_store.db")
    
    try {
        // Store some example events
        println("\n📝 Storing events...")
        
        val studentTag = StudentTag("student-001")
        val courseTag = CourseTag("course-chem-101")
        
        // Store student registration event
        val registrationEvent = StudentRegistered(
            student = studentTag,
            firstName = "John",
            lastName = "Doe", 
            email = "john.doe@example.com"
        )
        val envelope1 = eventStore.storeEvent(1L, registrationEvent)
        println("Stored: ${registrationEvent::class.simpleName} (seq: ${envelope1.sequence})")
        
        // Store student enrollment event
        val enrollmentEvent = Enrolled(studentTag, courseTag)
        val envelope2 = eventStore.storeEvent(2L, enrollmentEvent)
        println("Stored: ${enrollmentEvent::class.simpleName} (seq: ${envelope2.sequence})")
        
        // Store grade assignment event
        val gradeEvent = Graded(studentTag, courseTag, "A")
        val envelope3 = eventStore.storeEvent(3L, gradeEvent)
        println("Stored: ${gradeEvent::class.simpleName} (seq: ${envelope3.sequence})")
        
        // Query the event store
        println("\n🔍 Querying events...")
        
        // Get total count
        val totalEvents = eventStoreRepo.totalEventsStored()
        println("Total events stored: $totalEvents")
        
        // Find all events from beginning
        val allEvents = eventStoreRepo.findLastEvent(0)
        println("Found ${allEvents.size} events from index 0")
        
        // Find last event for a specific student
        val lastStudentEvent = eventStoreRepo.findLastTaggedEvent(studentTag)
        if (lastStudentEvent != null) {
            println("Last event for student ${studentTag.id}: ${lastStudentEvent.event::class.simpleName}")
        }
        
        // Find last grade event for the course
        val lastGradeEvent = eventStoreRepo.findLastTaggedEvent(
            Event.type<Graded>(), 
            courseTag
        )
        if (lastGradeEvent != null) {
            println("Last grade event for course ${courseTag.id}: ${lastGradeEvent.event::class.simpleName}")
        }
        
        // Display database statistics
        println("\n📊 Database Statistics:")
        if (eventStore is dev.oblac.eddi.sqlite.SqliteEventStore) {
            val stats = eventStore.stats()
            println("- Total Events: ${stats.totalEvents}")
            println("- Database Path: ${stats.databasePath}")
            println("- Oldest Event: ${stats.oldestEventTimestamp}")
            println("- Newest Event: ${stats.newestEventTimestamp}")
        }
        
        println("\n✅ SQLite Event Store example completed successfully!")
        
    } catch (e: Exception) {
        println("\n❌ Error occurred: ${e.message}")
        e.printStackTrace()
    } finally {
        // Clean up resources (optional - databases are automatically managed)
        if (eventStore is dev.oblac.eddi.sqlite.SqliteEventStore) {
            eventStore.close()
        }
        println("\n🔐 Database connections closed")
    }
}

/**
 * Alternative example showing in-memory SQLite usage for testing.
 */
fun memoryExample() = runBlocking {
    println("=== In-Memory SQLite Example ===\n")
    
    // Create an in-memory database for testing
    val (eventStore, eventStoreRepo) = SqliteEddiFactory.createInMemoryEventStore()
    
    println("✅ In-memory SQLite Event Store initialized")
    
    // Store and query events (same as above but in memory)
    val studentTag = StudentTag("test-student")
    val courseTag = CourseTag("test-course")
    
    val event = StudentRegistered(studentTag, "Test", "Student", "test@example.com")
    eventStore.storeEvent(1L, event)
    
    val totalEvents = eventStoreRepo.totalEventsStored()
    println("Events stored in memory: $totalEvents")
    
    println("✅ In-memory example completed")
}