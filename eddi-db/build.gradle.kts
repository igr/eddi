plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(project(":eddi-api"))
    implementation(project(":eddi-json"))
    implementation(libs.kotlinx.coroutines)
    implementation(kotlin("reflect"))

    // Exposed ORM
    implementation(libs.bundles.exposed)

    // Database
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation(libs.bundles.flyway)

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
}
