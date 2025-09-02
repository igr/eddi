plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":eddi-api"))
    implementation(libs.kotlinx.coroutines)
    implementation(kotlin("reflect"))
    
    // Exposed ORM
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    
    // SQLite JDBC driver
    implementation(libs.sqlite.jdbc)
    
    // Jackson JSON serialization
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.datatype.jsr310)

    // Testing
    testImplementation(libs.junit.jupiter.engine)
}