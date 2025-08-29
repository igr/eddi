plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    version = "1.0.0-SNAPSHOT"
    group = "dev.oblac.eddi"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.apply("java-library")
    
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        tasks.named<Test>("test") {
            useJUnitPlatform()
        }
    }
}

//tasks.register('clean', Delete) {
//    delete rootProject.layout.buildDirectory
//}

