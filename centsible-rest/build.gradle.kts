plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":centsible-core"))
    implementation(project(":centsible-api"))
    implementation(project(":centsible-proto"))

    // Integration provider modules. To remove a provider, delete its line — nothing else changes.
    // Each provider module pulls :centsible-integrations:support transitively for shared HTTP +
    // string helpers; no need to declare it here.
    implementation(project(":centsible-integrations:manual"))
    implementation(project(":centsible-integrations:paypal"))
    implementation(project(":centsible-integrations:banking-gocardless"))

    // File-import modules. Core defines the SPI; format sub-modules (csv, ofx, ...) register
    // themselves via Spring component scanning. To remove a format, delete its line.
    implementation(project(":centsible-imports:core"))
    implementation(project(":centsible-imports:csv"))
    implementation(project(":centsible-imports:ofx"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.jooq)
    implementation(libs.spring.boot.starter.validation)

    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)
    runtimeOnly(libs.postgresql)

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    implementation(libs.bucket4j.core)
    implementation(libs.caffeine)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.spring.boot.starter.jooq.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
