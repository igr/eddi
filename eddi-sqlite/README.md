# Eddi SQLite Module

The `eddi-sqlite` module provides SQLite-based implementations of Eddi's event storage interfaces using the Exposed Kotlin ORM library. This module is designed for high-performance, persistent event storage with ACID guarantees.

## Features

- **Persistent Storage**: Events are stored in SQLite database files with full ACID guarantees
- **High Performance**: Optimized with proper indexing, WAL mode, and connection pooling
- **Thread Safe**: Built with Kotlin coroutines and proper concurrency handling
- **JSON Serialization**: Automatic serialization/deserialization of Event objects and Tag history
- **Append-Only Design**: Optimized table structure for high-throughput event storage
- **Connection Management**: Automatic database initialization and connection handling
- **Error Handling**: Comprehensive error handling with retry logic for database operations

## Dependencies

The module uses the following key dependencies:
- **Exposed ORM**: For database operations and schema management
- **SQLite JDBC**: SQLite database driver
- **Kotlinx Serialization**: For JSON serialization of complex objects
- **Kotlinx Coroutines**: For async operations and thread safety

## Quick Start

### Basic Usage

```kotlin
import dev.oblac.eddi.sqlite.SqliteEddiFactory

// Create SQLite Event Store components
val (eventStore, eventStoreRepo) = SqliteEddiFactory.createEventStore(
    databasePath = "my_events.db"
)

// Store an event
val event = MyEvent(data = "example")
val envelope = eventStore.storeEvent(correlationId = 1L, event)

// Query events
val totalEvents = eventStoreRepo.totalEventsStored()
val recentEvents = eventStoreRepo.findLastEvent(fromIndex = 0)
val taggedEvent = eventStoreRepo.findLastTaggedEvent(myTag)
```

### Configuration Builder

```kotlin
val (eventStore, eventStoreRepo) = SqliteEddiFactory.builder()
    .databasePath("custom_path/events.db")
    .createDirectories(true)
    .build()
```

### In-Memory Database (for testing)

```kotlin
val (eventStore, eventStoreRepo) = SqliteEddiFactory.createInMemoryEventStore()
```

## Database Schema

The module creates a single table `event_envelopes` with the following structure:

| Column | Type | Description |
|--------|------|-------------|
| `sequence` | BIGINT | Auto-increment primary key |
| `correlation_id` | BIGINT | Correlation ID for event tracking |
| `event_type` | VARCHAR(255) | Fully qualified event class name |
| `event_json` | TEXT | Serialized Event object as JSON |
| `history_json` | TEXT | Serialized Tag history as JSON |
| `timestamp` | TIMESTAMP | Event creation timestamp |

### Indexes

The following indexes are automatically created for optimal query performance:
- Primary key on `sequence`
- Index on `event_type`
- Index on `timestamp`
- Composite index on `event_type` + `timestamp`
- Index on `correlation_id`

## Advanced Features

### Batch Operations

```kotlin
suspend fun storeManyEvents(events: List<Event>) {
    val envelopes = events.mapIndexed { index, event ->
        eventStore.storeEvent(correlationId = index.toLong(), event)
    }
}
```

### Database Statistics

```kotlin
if (eventStore is SqliteEventStore) {
    val stats = eventStore.getStats()
    println("Total Events: ${stats.totalEvents}")
    println("Database Path: ${stats.databasePath}")
    println("Oldest Event: ${stats.oldestEventTimestamp}")
    println("Newest Event: ${stats.newestEventTimestamp}")
}
```

### Custom Database Configuration

The module automatically configures SQLite for optimal performance:
- **WAL Mode**: Enables better concurrent access
- **Synchronous=NORMAL**: Balanced performance and safety
- **Cache Size**: 10,000 pages for better memory usage
- **Memory-Mapped I/O**: 256MB for faster reads
- **Busy Timeout**: 30 seconds for handling locks

## Error Handling

The module includes comprehensive error handling:

- **Retry Logic**: Automatic retry for transient database errors
- **Custom Exceptions**: `SqliteEventStoreException` for all module-specific errors
- **Transaction Safety**: All operations are wrapped in database transactions
- **Connection Recovery**: Automatic handling of connection issues

## Performance Considerations

### Write Performance
- Uses batch insert operations for multiple events
- Append-only table design eliminates update overhead  
- WAL mode allows concurrent readers during writes
- Prepared statements for consistent performance

### Read Performance
- Strategic indexes on commonly queried columns
- Memory-mapped I/O for large databases
- Connection pooling managed by Exposed
- Optimized query patterns for tag-based lookups

### Storage
- JSON serialization keeps the schema flexible
- Vacuum operations can be run manually when needed
- Database file can be backed up while running (WAL mode)

## Thread Safety

All operations are thread-safe through:
- Kotlin coroutines with proper dispatchers
- Database connection pooling via Exposed
- Mutex protection for critical initialization sections
- ACID guarantees from SQLite transactions

## Migration and Compatibility

The module handles schema evolution automatically:
- Database schema is created on first access
- Tables are created if they don't exist
- Future versions will include migration support

## Examples

Complete examples are available in the `example` module:
- Basic event storage and retrieval
- Tag-based event queries
- Database statistics monitoring
- In-memory database usage for testing

## Best Practices

1. **Connection Management**: Use the factory methods to ensure proper setup
2. **Error Handling**: Wrap operations in try-catch blocks for `SqliteEventStoreException`
3. **Performance**: Use batch operations for multiple events when possible
4. **Testing**: Use in-memory databases for unit tests
5. **Backup**: SQLite databases can be backed up while running in WAL mode
6. **Monitoring**: Periodically check database statistics for performance insights

## Integration with Eddi Framework

The SQLite module seamlessly integrates with other Eddi components:

```kotlin
val eddi = Eddi(
    commandBus = memoryCommandBus,
    commandStore = memoryCommandStore,
    eventBus = memoryEventBus,
    evetStore = sqliteEventStore,        // Use SQLite for events
    evetStoreRepo = sqliteEventStoreRepo, // Use SQLite for queries
    eventStoreOutbox = memoryEventStoreOutbox,
    serviceRegistry = memoryServiceRegistry,
    projector = memoryProjector
)
```

This allows you to use persistent SQLite storage for events while keeping other components in memory for optimal performance.