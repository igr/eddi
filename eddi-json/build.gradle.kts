plugins {
    alias(libs.plugins.kotlin.jvm)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":eddi-api"))

    // JSON serialization
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.datatype.jsr310)

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter.engine)
    testImplementation("io.github.serpro69:kotlin-faker:2.0.0-rc.11")
}
