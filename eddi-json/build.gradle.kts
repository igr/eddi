plugins {
    alias(libs.plugins.kotlin.jvm)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":eddi-api"))
    implementation(libs.kotlin.reflect)

    // JSON serialization
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.datatype.jsr310)
}
