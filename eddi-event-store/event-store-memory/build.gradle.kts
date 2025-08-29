plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(project(":eddi-api"))
    implementation(project(":eddi-event-store:event-store-api"))
    implementation(project(":eddi-event-bus:event-bus-api"))
    implementation(libs.kotlinx.coroutines)
}