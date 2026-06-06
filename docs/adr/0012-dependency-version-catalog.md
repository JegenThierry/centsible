# ADR-0012: Dependency Versions via the Gradle Version Catalog

Status:  Accepted
Date:    2026-06-06
Scope:   build

## Context

A multi-module build that hardcodes `group:artifact:version` in each module drifts: the
same library ends up at different versions in different modules, and upgrades mean hunting
through every build file.

## Decision

- Declare **every** dependency and plugin in `gradle/libs.versions.toml` and reference it
  through the **`libs.*` aliases**.
- Module build files use `implementation(libs.…)` and `alias(libs.plugins.…)` — they do
  **not** contain hardcoded version strings.

If the catalog already has an alias for a library, use it — don't reintroduce a hardcoded
coordinate string alongside it.

## Examples

**Do**

```kotlin
plugins { alias(libs.plugins.kotlin.spring) }
dependencies {
    implementation(libs.spring.boot.starter.web)
    testImplementation(libs.kotlin.test.junit5)
}
```

**Don't**

```kotlin
testImplementation("org.springframework.boot:spring-boot-starter-test") // hardcoded — avoid
```

Instead, reference the catalog alias (`libs.spring.boot.starter.test`); add one to
`libs.versions.toml` only if it doesn't exist yet.

## Consequences

- One place to see and bump every version; modules stay consistent automatically.
- The OWASP dependency scan and upgrades operate on a single source of truth.
- Cost: adding a new library is two steps (catalog entry, then alias use) instead of one
  inline string.
