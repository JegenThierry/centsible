plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dependency.management)
}

dependencies {
    api(project(":budget-planner-api"))
    implementation(project(":budget-planner-jooq"))

    implementation(libs.spring.boot.starter)
    implementation(libs.spring.tx)
    implementation(libs.spring.web)
    implementation(libs.spring.security.crypto)

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
