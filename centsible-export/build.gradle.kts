plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":centsible-jooq"))
    implementation(project(":centsible-api"))
    implementation(project(":centsible-proto"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.jooq)

    implementation(libs.kotlin.reflect)
    implementation(libs.jackson3.module.kotlin)
    implementation(libs.jackson3.databind)
    runtimeOnly(libs.postgresql)

    implementation(libs.pebble)
    implementation(libs.playwright)
    implementation(libs.commons.csv)
    implementation(libs.resilience4j.retry)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
