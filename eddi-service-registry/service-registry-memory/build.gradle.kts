plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(project(":eddi-api"))
    implementation(project(":eddi-service-registry:service-registry-api"))
    implementation(project(":eddi-cmd-bus:cmd-bus-api"))    // todo should be a transitive dependency from service-registry-api
    implementation(project(":eddi-event-store:event-store-api"))
}
