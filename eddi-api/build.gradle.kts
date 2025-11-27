plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    api(libs.kotlinx.coroutines)
    runtimeOnly(libs.kotlin.reflect)
}
