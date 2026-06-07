plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dependency.management)
    alias(libs.plugins.ksp)
}

dependencies {
    api(project(":centsible-api"))
    implementation(project(":centsible-jooq"))
    implementation(project(":centsible-imports:core"))

    implementation(libs.spring.boot.starter)
    implementation(libs.spring.tx)
    implementation(libs.spring.web)
    implementation(libs.spring.security.crypto)
    implementation(libs.jackson.databind)
    implementation(libs.caffeine)
    implementation(libs.totp)
    implementation(libs.konvert.api)
    ksp(libs.konvert.ksp)

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
