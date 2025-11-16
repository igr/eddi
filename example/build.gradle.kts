plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("dev.oblac.eddi.example.ExampleKt")
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(project(":eddi-api"))
    implementation(project(":eddi-db"))
}
