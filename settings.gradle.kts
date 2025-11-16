plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "eddi"

include("eddi-api")
include("eddi-db")
include("example")