plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    api(libs.kotlinx.coroutines)
    api(libs.arrow.core)
    runtimeOnly(libs.kotlin.reflect)
}
