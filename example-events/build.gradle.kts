plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.google.devtools.ksp")
    application
}

dependencies {
    implementation(project(":eddi-api"))
    ksp(project(":eddi-ksp"))
}
