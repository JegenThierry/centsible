# ADR-0013: Plain-SQL Migrations + Committed jOOQ Codegen

Status:  Accepted
Date:    2026-06-06
Scope:   backend · database

## Context

The project uses jOOQ, whose generated sources must match the live schema, but deliberately
avoids Flyway/Liquibase. The build must stay runnable without a database, and schema drift
between the DB and the generated code must be impossible to miss.

## Decision

- The base schema is `centsible-db/tables.sql`. Schema changes are **ordered SQL migration
  files** under `centsible-db/migrations/<version>/NN_name.sql`.
- `centsible-db/run-migrations.sh` applies them **idempotently**, tracking applied versions
  in a `schema_migrations` table (re-running is safe). In deployment a one-shot
  `centsible-migrations` container runs it on every `docker compose up`.
- **jOOQ generated sources are committed** under `centsible-jooq/src/generated/jooq/` and
  registered as a source dir, so `./gradlew build` needs **no** database. Codegen is a
  separate, manual step.

**After any schema change, in order:**
1. write a migration file,
2. apply it to your local DB (`run-migrations.sh`),
3. run `./gradlew :centsible-jooq:jooqCodegen` and **commit** the regenerated sources.

## Examples

**Do**
- Add `centsible-db/migrations/0.3.0/03_add_budget_archived_flag.sql`, apply it, regenerate
  jOOQ, and commit both the migration and the changed generated records together.

**Don't**
- Edit `tables.sql` for an existing deployment instead of adding a migration.
- Change the schema and skip `jooqCodegen` — the generated records will no longer match the
  DB and the mismatch won't show until runtime.
- Hand-edit anything under `src/generated/jooq/`.

## Consequences

- The build is DB-free and reproducible; the generated layer is always in version control
  alongside the migration that caused it.
- Migrations are safe to re-run and ordered.
- Cost: a schema change is a three-step ritual, and the generated diff must be committed.
  Keep the discipline — it's what keeps codegen honest.
