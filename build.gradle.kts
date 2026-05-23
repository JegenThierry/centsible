plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dependency.management) apply false
    alias(libs.plugins.dependency.check)
}

// OWASP Dependency-Check. Run `./gradlew dependencyCheckAggregate` — walks every
// subproject's resolved dependencies and writes reports to build/reports/dependency-check/.
// First run downloads the NVD feed (slow); supply an NVD API key
// (https://nvd.nist.gov/developers/request-an-api-key) to lift the anonymous rate limit.
// Resolution order: -PnvdApiKey=… > ~/.gradle/gradle.properties (nvdApiKey=…) > NVD_API_KEY env var.
// Opt-in policy gate: `-PdependencyCheckFailOnCvss=7.0` to fail the build on High+ CVSS.
dependencyCheck {
    formats = listOf("HTML", "JSON")
    outputDirectory = layout.buildDirectory.dir("reports/dependency-check").get().asFile.absolutePath

    nvd.apiKey = (findProperty("nvdApiKey") as String?)
        ?: System.getenv("NVD_API_KEY")
        ?: ""

    val suppression = file("dependency-check-suppression.xml")
    if (suppression.exists()) {
        suppressionFile = suppression.absolutePath
    }

    analyzers.assemblyEnabled = false
    analyzers.nuspecEnabled = false
    analyzers.nugetconfEnabled = false

    analyzers.nodeEnabled = false
    analyzers.nodeAuditEnabled = false
    analyzers.retirejs.enabled = false

    analyzers.ossIndex.enabled = false
    analyzers.ossIndex.warnOnlyOnRemoteErrors = true

    (findProperty("dependencyCheckFailOnCvss") as String?)?.toFloatOrNull()?.let {
        failBuildOnCVSS = it
    }
}

val guavaVersion = libs.versions.guava.get()

allprojects {
    group = "beer.thierry"
    version = "0.3.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "project-report")

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES) {
                // Override BOM versions to pull CVE fixes ahead of the next Spring Boot release.
                bomProperty("tomcat.version", "11.0.22")          // CVE-2026-43512, CVE-2026-41293, et al.
                bomProperty("postgresql.version", "42.7.11")      // CVE-2026-42198 (SCRAM PBKDF2 DoS)
            }
        }
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        constraints {
            "implementation"("com.google.guava:guava:$guavaVersion")
            "runtimeOnly"("com.google.guava:guava:$guavaVersion")
        }
    }
}

tasks.register("dependencyReportAll") {
    group = "reporting"
    description = "Generates HTML dependency reports for every subproject under each module's build/reports/project/dependencies/."
    dependsOn(subprojects.map { "${it.path}:htmlDependencyReport" })
}
