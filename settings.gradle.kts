plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "eddi"

include("eddi-api")
include("eddi-cmd-store")
include("eddi-cmd-store:cmd-store-api")
include("eddi-cmd-store:cmd-store-memory")
include("eddi-cmd-bus")
include("eddi-cmd-bus:cmd-bus-api")
include("eddi-cmd-bus:cmd-bus-memory")
include("eddi-event-bus")
include("eddi-event-bus:event-bus-api")
include("eddi-event-bus:event-bus-memory")
include("eddi-service-registry")
include("eddi-service-registry:service-registry-api")
include("eddi-service-registry:service-registry-memory")
include("eddi-event-store")
include("eddi-event-store:event-store-api")
include("eddi-event-store:event-store-memory")
include("eddi-projector")
include("eddi-projector:projector-api")
include("eddi-projector:projector-memory")
include("example")