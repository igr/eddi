plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(project(":eddi-api"))
    implementation(project(":eddi-projector:projector-api"))
    implementation(project(":eddi-event-bus:event-bus-api"))
}