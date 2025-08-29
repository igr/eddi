plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":eddi-api"))

    implementation(project(":eddi-cmd-store:cmd-store-api"))
    api(project(":eddi-cmd-store:cmd-store-memory"))

    implementation(project(":eddi-cmd-bus:cmd-bus-api"))
    api(project(":eddi-cmd-bus:cmd-bus-memory"))

    implementation(project(":eddi-service-registry:service-registry-api"))
    api(project(":eddi-service-registry:service-registry-memory"))

    implementation(project(":eddi-event-bus:event-bus-api"))
    api(project(":eddi-event-bus:event-bus-memory"))

    implementation(project(":eddi-event-store:event-store-api"))
    api(project(":eddi-event-store:event-store-memory"))

    implementation(project(":eddi-projector:projector-api"))
    api(project(":eddi-projector:projector-memory"))
}
