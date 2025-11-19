plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.google.devtools.ksp")
    application
}

application {
    mainClass.set("dev.oblac.eddi.example.ExampleKt")
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(project(":eddi-api"))
    implementation(project(":eddi-db"))
    ksp(project(":eddi-ksp"))
}
