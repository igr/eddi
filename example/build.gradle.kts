plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

application {
    mainClass.set("dev.oblac.eddi.example.college.MainKt")
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(project(":eddi-api"))
    implementation(project(":eddi-db"))
    implementation(project(":example-events"))

    val mordantVersion = "3.0.2"
    implementation("com.github.ajalt.mordant:mordant:${mordantVersion}")
    implementation("com.github.ajalt.mordant:mordant-coroutines:${mordantVersion}")
    implementation("com.github.ajalt.mordant:mordant-markdown:${mordantVersion}")
}
