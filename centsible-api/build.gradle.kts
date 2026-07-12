dependencies {
    api(libs.spring.boot.starter.security)
    api(libs.spring.boot.starter.validation)

    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
