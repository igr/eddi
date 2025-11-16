plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(project(":eddi-api"))
    implementation(libs.kotlinx.coroutines)
    implementation(kotlin("reflect"))

    // JSON serialization
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.datatype.jsr310)

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:0.56.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.56.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.56.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.56.0")
    implementation("org.jetbrains.exposed:exposed-json:0.56.0")

    // Database
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.flywaydb:flyway-core:10.21.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.21.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
}
