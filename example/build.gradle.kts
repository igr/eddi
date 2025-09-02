plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("dev.oblac.eddi.example.ExampleKt")
}

dependencies {
    implementation(project(":eddi-api"))
    implementation(project(":eddi-memory"))
    implementation(project(":eddi-sqlite"))
    implementation(libs.kotlinx.coroutines)
}
