plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

application {
    mainClass.set("dev.oblac.eddi.example.college.MainKt")
}

tasks.register("printClasspath") {
    doLast {
        println(sourceSets["main"].runtimeClasspath.asPath)
    }
}

dependencies {
    implementation(project(":eddi-api"))
    implementation(project(":eddi-json"))
    implementation(project(":eddi-db"))
    implementation(project(":example-events"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.html.builder)
}
