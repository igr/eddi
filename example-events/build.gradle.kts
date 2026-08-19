plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":eddi-api"))
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
}
