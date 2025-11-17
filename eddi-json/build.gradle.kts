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
}
