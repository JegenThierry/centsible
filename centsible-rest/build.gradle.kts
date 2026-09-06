plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":centsible-core"))
    implementation(project(":centsible-api"))
    implementation(project(":centsible-proto"))

    implementation(project(":centsible-imports:core"))
    implementation(project(":centsible-imports:csv"))
    implementation(project(":centsible-imports:ofx"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.jooq)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.data.commons)

    implementation(libs.kotlin.reflect)
    implementation(libs.jackson3.databind)
    implementation(libs.jackson3.module.kotlin)
    runtimeOnly(libs.postgresql)

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    implementation(libs.bucket4j.core)
    implementation(libs.caffeine)
    implementation(libs.tika.core)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.boot.starter.jooq.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
