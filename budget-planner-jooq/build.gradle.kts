import java.util.*

plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dependency.management)
    alias(libs.plugins.jooq.codegen)
}

val props = Properties().apply {
    val propsFile = file("src/main/resources/jooq.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use { load(it) }
    }
}

fun resolveProperty(key: String): String? {
    val rawValue = props.getProperty(key) ?: return null
    val match = Regex("""\$\{([^:]+)(?::([^}]*))?\}""").find(rawValue)
    return if (match != null) {
        val envVar = match.groupValues[1]
        val defaultValue = match.groupValues[2]
        System.getenv(envVar) ?: defaultValue
    } else {
        rawValue
    }
}

val jooqGeneratedDir = layout.projectDirectory.dir("src/generated/jooq")

dependencies {
    api(project(":budget-planner-api"))

    implementation(libs.spring.boot.starter.jooq)
    implementation(libs.spring.security.crypto)
    runtimeOnly(libs.postgresql)

    jooqCodegen(libs.postgresql)
}

jooq {
    configuration {
        jdbc {
            driver = resolveProperty("jooq.datasource.driver-class-name") ?: "org.postgresql.Driver"
            url = resolveProperty("jooq.datasource.url")
            user = resolveProperty("jooq.datasource.username")
            password = resolveProperty("jooq.datasource.password")
        }
        generator {
            name = "org.jooq.codegen.KotlinGenerator"
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                inputSchema = "public"
            }
            generate {
                isDeprecated = false
                isRecords = true
                isImmutablePojos = true
                isFluentSetters = true
            }
            target {
                packageName = "beer.thierry.jooq.generated"
                directory = jooqGeneratedDir.asFile.path
            }
        }
    }
}

sourceSets {
    main {
        kotlin {
            srcDir(jooqGeneratedDir)
        }
    }
}
