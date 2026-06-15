# AGENTS.md — Centsible

## Project shape

Monorepo: Nuxt 4 frontend + Spring Boot 4/Kotlin backend. Two runnable Spring Boot services (`centsible-rest`, `centsible-export`). PostgreSQL 18, jOOQ, Gradle multi-module (Kotlin DSL), Docker Compose for deployment only.

Backend layer dependency flow: `rest` → `core` → `jooq` → `api` (and `proto` shared between `rest` and `export`).

## Commands

```bash
# Backend (run from repo root)
./gradlew :centsible-rest:bootRun          # REST API
./gradlew :centsible-export:bootRun        # Export worker
./gradlew build                            # Build all + run tests (tests need Postgres)
./gradlew :centsible-jooq:jooqCodegen      # Regenerate jOOQ from live DB schema
./gradlew :centsible-proto:generateProto   # Regenerate protobuf classes
./gradlew dependencyCheckAggregate         # OWASP vuln scan

# Frontend (run from centsible-ui/)
npm run dev                                # Dev server
npm run build                              # Production build
npx tsc --noEmit -p tsconfig.json          # Type check (what CI runs)
```

## Schema changes (critical ritual)

After any DB schema change, **in order**:
1. Write migration file under `centsible-db/migrations/<version>/NN_name.sql`
2. Apply it to your local DB (`centsible-db/run-migrations.sh`)
3. Run `./gradlew :centsible-jooq:jooqCodegen` and **commit** the regenerated sources

jOOQ generated sources are **committed** under `centsible-jooq/src/generated/jooq/`. Never hand-edit them. The build needs no database because generated code is in VCS. See `docs/adr/0013-sql-migrations-and-jooq-codegen.md`.

## Secrets & config

- `application.properties` files are gitignored and **blocked by a pre-commit hook**. Only commit `.example` versions.
- The pre-commit hook must be enabled per clone: `git config core.hooksPath .githooks`
- `.env` is gitignored; copy `.env.example` and fill in required values.
- `ProductionGuard` enforces invariants under `SPRING_PROFILES_ACTIVE=prod` (e.g. `AUTH_COOKIE_SECURE` must be true, `SKIP_EMAIL_VERIFICATION` must be false).

## Local dev prereqs

- JDK 21, Node.js 20+, PostgreSQL 18
- Create `centsible` database, run `centsible-db/run-migrations.sh`
- Copy `application.properties.example` → `application.properties` in `centsible-rest/src/main/resources/` and `centsible-export/src/main/resources/`
- Run both `bootRun` commands + `npm run dev` in `centsible-ui/`

## Testing

- Backend tests: `./gradlew test` (requires a running Postgres with the schema applied; CI skips tests with `-x test` for now)
- Frontend: no test suite yet. CI only runs `tsc --noEmit` type check.
- API test collections: `centsible-bruno/` (Bruno client); testdata seeder at `centsible-bruno/testdata/`

## Code style

- Kotlin/Java: 4-space indent. Vue/TS/JS/JSON: 2-space indent. LF line endings.
- `Konvert` KSP annotation processor handles DTO mapping in `centsible-core`.
- Integrations use a plugin SPI: each provider is a submodule under `centsible-integrations/`. Import formats live under `centsible-imports/`.
- ADRs in `docs/adr/` record architecture decisions — read them before making large structural changes.
