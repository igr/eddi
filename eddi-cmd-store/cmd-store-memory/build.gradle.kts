plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(project(":eddi-api"))
    implementation(project(":eddi-cmd-store:cmd-store-api"))
    implementation(project(":eddi-cmd-bus:cmd-bus-api"))
    implementation(libs.kotlinx.coroutines)
}
