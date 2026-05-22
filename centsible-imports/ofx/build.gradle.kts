plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":centsible-api"))
    implementation(project(":centsible-imports:core"))

    implementation(libs.spring.boot.starter)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
