# ADR-0001: Onion Architecture — Dependencies Point Inward Only

Status:  Accepted
Date:    2026-06-06
Scope:   backend

## Context

The backend is a multi-module Gradle build. Without a hard rule about which module may
depend on which, inner modules drift into depending on outer ones (a service reaching
for a jOOQ record, or the contract layer importing Spring MVC), which makes the core
business logic impossible to test or reason about in isolation and couples everything to
the web/persistence frameworks.

## Decision

Module dependencies point **inward only**:

```
rest  →  core  →  jooq  →  api
```

- `centsible-api` is the contract layer and depends on **nothing** but the
  validation/security starters. It holds DTOs (`model/<domain>/`), service interfaces
  (`services/<domain>/IXService`), repository interfaces (`repository/IXRepository`), and
  shared exceptions.
- Each layer talks to the next **only through interfaces defined in `centsible-api`**.
  An inner layer must never import a type from an outer layer.
- `centsible-jooq` implements `api.repository.*`; `centsible-core` implements
  `api.services.*`; `centsible-rest` is the Spring Boot entry point.

`rest` and the separate `centsible-export` app share only `api`, `jooq`, and `proto`.

## Examples

**Do** — `centsible-core/build.gradle.kts` exposes the contract and hides persistence:

```kotlin
dependencies {
    api(project(":centsible-api"))            // contract is part of core's public API
    implementation(project(":centsible-jooq")) // SQL impl is an internal detail
}
```

`centsible-jooq/build.gradle.kts` depends only inward: `api(project(":centsible-api"))`.

**Don't**
- Import a generated `*Record` or a jOOQ `DSLContext` from `centsible-core` or
  `centsible-api`.
- Reference a `*Resource` controller, `HttpServletRequest`, or anything Spring-MVC from
  `core`/`jooq`/`api`.
- Add a `dependency` line that points outward (e.g. `core` depending on `rest`).

## Consequences

- Core business rules are unit-testable with mocked repository interfaces — no DB, no web
  container (see the `*ServiceTest` classes).
- The persistence and web frameworks are swappable behind interfaces.
- Cost: every cross-layer call needs an interface in `centsible-api`, so adding a method
  means touching the contract first. This is intentional friction.

Related: [ADR-0002](0002-layered-naming-and-domain-packaging.md),
[ADR-0011](0011-single-responsibility-across-layers.md).
