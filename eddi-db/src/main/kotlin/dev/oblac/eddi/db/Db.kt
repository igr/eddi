package dev.oblac.eddi.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

data class MigrationConfig(
    val schema: String,
    val location: String = "classpath:db/migration/$schema",
    val tableName: String = "flyway_schema_history"
)

class Db(
    jdbcUrl: String,
    username: String,
    password: String,
    maximumPoolSize: Int = 10,
    migrations: List<MigrationConfig> = listOf(MigrationConfig(schema = "eddi"))
) {
    private val dataSource: HikariDataSource

    init {
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            this.maximumPoolSize = maximumPoolSize
            this.driverClassName = "org.postgresql.Driver"
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        // Run Flyway migrations for each module independently
        migrations.forEach { migrationConfig ->
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(migrationConfig.schema)
                .locations(migrationConfig.location)
                .table(migrationConfig.tableName)
                .load()

            flyway.migrate()
        }
    }

    fun close() {
        dataSource.close()
    }
}
