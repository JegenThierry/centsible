# Changelog

All notable changes to **Centsible** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] — 2026-05-16

First public release. A self-hostable personal-finance tracker with multi-account
balance tracking, categorized transactions, monthly budgets, lending/borrowing,
CSV import, recurring transactions, dashboard charts, and a multilingual UI.

### Added

#### Core finance features
- Accounts with computed running balances via the `account_history` view, derived
  from `initial_balance` plus signed transaction amounts.
- Categories (income/expense) with a global unique-name constraint.
- Transactions with full CRUD and per-account/per-category filtering.
- Monthly per-category budgets with a dashboard overview (#14).
- Recurring transactions backed by a daily scheduler (#13).
- Lending and borrowing management with contacts, loans, and repayments (#5).
- CSV transaction import with column mapping and deduplication (#11).
- Data export service for full backups of user data (#7).
- Dashboard charts: spending-by-category and income-vs-expense (#12).

#### Auth & security
- Stateless JWT auth issued as an `HttpOnly; Secure; SameSite=Strict` cookie,
  with `Authorization: Bearer` fallback for CLI use.
- User registration with email confirmation via Resend (#2), gated by
  `REGISTRATION_ENABLED` and short-circuitable via `SKIP_EMAIL_VERIFICATION`
  for local dev.
- Hashed registration tokens with expiration, IP-based rate limiting on
  unauthenticated auth endpoints (10 req/min/IP), and secure auth cookies (#10).
- Image upload validation hardening (#10).
- `ProductionGuard` prevents the rest module from booting with
  `SKIP_EMAIL_VERIFICATION=true` under the `prod` profile.

#### Integrations
- Provider-connection framework: database schema, encrypted credential storage,
  REST endpoints, service layer, and an integration registry (#9).

#### Frontend
- Nuxt 4 / Vue 3 UI with a side navigation shell (#1).
- Centralized form validation pattern paired with Jakarta Bean Validation on
  DTOs (#4).
- About page surfacing backend/system information (#6).
- Landing page with hero, features, and CTA sections (#15).
- Theme selector with predefined palettes and light/dark modes, plus a dynamic
  tinted-neutral palette system for consistent theming.
- Account confirmation page and refreshed email confirmation flow.
- Centsible rebrand: updated UI, branding, and metadata.
- Multilingual support with English, German, and French locales (#16).

#### Tooling & ops
- Production deployment overlay `docker-compose.prod.yml`: builds the UI from
  the compiled `release` stage, drops watch/source mounts, unpublishes host
  ports, and attaches REST + UI to an external `proxy-net` so a reverse proxy
  (nginx-proxy-manager, Caddy, Traefik) can terminate TLS and route traffic by
  service name.
- Gradle multi-module backend (`api` → `jooq` → `core` → `rest`) with a strict
  one-way dependency graph; version catalog in `gradle/libs.versions.toml`.
- jOOQ code generation against a live Postgres, with generated sources
  committed under `centsible-jooq/src/generated/jooq`.
- File-ordered SQL migrations tracked in `schema_migrations`, executed by
  `centsible-db/run-migrations.sh`.
- Docker Compose orchestration of backend, UI, Postgres, and migrations, with
  `--watch` hot-reload for development.
- Bruno API collections per domain (`auth`, `budget-accounts`, `categories`,
  `transactions`, `users`) plus comprehensive test-data seeding (#8).
- N-layer architecture refactor removing leaked auth models and stale jOOQ
  artifacts (#3).

### Security
- Auth cookies require TLS in production (`Secure` flag).
- Postgres bound to `127.0.0.1` in `docker-compose.yml` — never exposed
  externally.
- All endpoints except `/api/auth/{register,login,confirm,logout}`,
  `/api/system`, the OAuth provider callback, and actuator `health` require
  a valid JWT.

[0.1.0]: https://codeberg.org/thierryjegen/budget-planner/releases/tag/v0.1.0
