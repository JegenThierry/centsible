# Centsible

A clean, self-hosted budget tracker — multiple accounts, smart categories, monthly budgets, and dashboards that actually help you decide. **NOTE: This project is in its early development stages, and bugs may occur. If you find any issues, please feel free to create an issue. When deploying use the main Branch or one of the released versions, develop may be buggy.**

A testing instance can be found under: <a href="https://centsible.thierry.beer">https://centsible.thierry.beer</a>

## Donate

Feel free to buy me a ☕.

<p>
  <a href="https://ko-fi.com/thierryjegen">
    <img src="https://ko-fi.com/img/githubbutton_sm.svg" alt="ko-fi" />
  </a>
</p>

## Project Structure & Architecture

The project is organized as a monorepo containing both frontend and backend modules. It follows a modular **N-Layer Architecture** (also known as Onion or Hexagonal-lite architecture) to ensure a clean separation of concerns, maintainability, and testability.

### Folder Overview

- **`centsible-api`**: Shared domain models, DTOs, and interfaces.
- **`centsible-bruno`**: API request collections for the [Bruno](https://www.usebruno.com/) API client.
- **`centsible-core`**: Core business logic and service implementations.
- **`centsible-db`**: Database schema, migrations, and initialization scripts.
- **`centsible-export`**: Standalone Spring Boot worker that picks up queued export jobs, renders PDFs via Pebble templates + Playwright, and runs post-processors (e.g. email delivery).
- **`centsible-jooq`**: Data access layer powered by jOOQ.
- **`centsible-proto`**: Protobuf schema (`export.proto`) shared between `rest` and `export` to describe the export job payload.
- **`centsible-rest`**: Spring Boot entry point and REST API controllers.
- **`centsible-ui`**: Nuxt.js frontend application.

### N-Layer Architecture (Backend)

The backend is decomposed into several modules, each representing a specific layer in the architecture. Dependency flow is strictly unidirectional (from outer layers to inner layers):

1.  **Presentation Layer (`centsible-rest`)**:
    - The entry point of the application.
    - Contains Spring Boot configuration, Security setup, and REST Controllers.
    - Responsible for handling HTTP requests, input validation, and mapping to/from DTOs.
    - Builds protobuf export payloads (via `centsible-proto`) and enqueues jobs that the export worker consumes.
    - **Depends on**: `centsible-core`, `centsible-api`, `centsible-proto`.

2.  **Business Layer (`centsible-core`)**:
    - Contains the "heart" of the application: services, facades, and business rules.
    - Orchestrates transactions and coordinates data flow between the API and Persistence layers.
    - **Depends on**: `centsible-jooq`, `centsible-api`.

3.  **Data Access Layer (`centsible-jooq`)**:
    - Handles all database interactions.
    - Includes jOOQ-generated classes and custom repository implementations.
    - Encapsulates SQL logic and provides a clean interface for the Business layer.
    - **Depends on**: `centsible-api`.

4.  **Shared Layer (`centsible-api`)**:
    - A lightweight module containing common DTOs, interfaces, constants, and exceptions.
    - Used as a bridge for communication between all other modules.
    - **Depends on**: None.

In addition, two cross-cutting modules support the export pipeline:

- **`centsible-proto`**: Pure protobuf module — generates Java classes from `src/main/proto/export.proto`. Consumed by both `rest` (producer) and `export` (consumer) so the wire format stays in sync.
- **`centsible-export`**: A separate Spring Boot service that polls the `export_jobs` table, reads the protobuf payload, renders a PDF (Pebble + Playwright), and runs any registered post-processors. It runs as its own container alongside `rest` and shares the `jooq` + `api` modules.

## Tech Stack

| Component         | Technology                                  |
|:------------------|:--------------------------------------------|
| **Frontend**      | Nuxt 4, Vue 3, Pinia, Tailwind CSS, Nuxt UI |
| **Backend**       | Spring Boot 4.0.1, Kotlin, JDK 21           |
| **Database**      | PostgreSQL 18.3, jOOQ                       |
| **Export worker** | Spring Boot, Pebble, Playwright, Protobuf   |
| **Orchestration** | Docker, Docker Compose                      |
| **API Testing**   | Bruno                                       |

## Getting Started

### Prerequisites

**Deployment:**
- [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/).
- A reverse proxy (nginx-proxy-manager, Caddy, Traefik, …) to terminate TLS — see [Installation and Deployment](#installation-and-deployment).

**Local development** (run directly, not via Docker):
- [JDK 21](https://adoptium.net/) for the backend.
- [Node.js](https://nodejs.org/) (v20+) for the frontend.
- A PostgreSQL 18 instance.

### Configuration

Copy `.env.example` to `.env` and adjust values. The full set of variables (with annotations) is documented inline there; see also the **Environment Variables** section below for descriptions and defaults.

`.env` is for **deployment** and its defaults are production-shaped (registration disabled, `Secure` cookies, `SPRING_PROFILES_ACTIVE=prod`). The equivalent settings for running the app directly during development are covered under [Local development](#local-development).

### Installation and Deployment

Docker Compose is used for **deployment only** — there is no Docker-based dev mode (see [Local development](#local-development)). The stack runs behind a reverse proxy: **no application ports are published to the host**, and the UI and API are reached by service name over a shared external Docker network.

1. **Identify (or create) the reverse-proxy network.** `docker-compose.yml` attaches `centsible-ui` and `centsible-rest` to an *external* network named by `PROXY_NETWORK` (default `nginx-proxy-manager_default`). It must already exist — your proxy stack usually creates it (`docker network ls` to find it) — or create a dedicated one and point `PROXY_NETWORK` at it:
   ```bash
   docker network create proxy-net   # then set PROXY_NETWORK=proxy-net in .env
   ```
2. **Configure `.env`** (copy from `.env.example`; see [Configuration](#configuration)). `POSTGRES_PASSWORD` and `JWT_SECRET` are **required** — the stack refuses to start if either is unset.
3. **Build and start:**
   ```bash
   docker compose up -d --build
   ```

This starts PostgreSQL, a one-shot migration runner, the REST API, the export worker, and the UI. Postgres and the export worker stay internal to `centsible-net`; only `centsible-rest:8080` and `centsible-ui:3000` are reachable, and only by the reverse proxy over `proxy-net`.

The reverse proxy (nginx-proxy-manager, Caddy, Traefik, …) terminates TLS and forwards to `centsible-ui:3000` and `centsible-rest:8080`, passing `X-Forwarded-Proto: https` and `X-Forwarded-Host` (the REST service trusts these via `SERVER_FORWARD_HEADERS_STRATEGY=framework`). Pick one topology in your proxy config:

| Topology | Proxy routing | `.env` values |
|:---|:---|:---|
| Two subdomains | `app.example.com → centsible-ui:3000`<br/>`api.example.com → centsible-rest:8080` | `APP_BASE_URL=https://app.example.com`<br/>`CORS_ALLOWED_ORIGINS=https://app.example.com`<br/>`AUTH_COOKIE_DOMAIN=.example.com`<br/>`NUXT_PUBLIC_API_BASE=https://api.example.com/api` |
| Single host, path-based | `example.com/api/* → centsible-rest:8080`<br/>`example.com/* → centsible-ui:3000` | `APP_BASE_URL=https://example.com`<br/>`CORS_ALLOWED_ORIGINS=https://example.com`<br/>`AUTH_COOKIE_DOMAIN=`<br/>`NUXT_PUBLIC_API_BASE=/api` |

Either way, `AUTH_COOKIE_SECURE=true` and `SPRING_PROFILES_ACTIVE=prod` must be set (ProductionGuard enforces this; both are the defaults in `.env.example`).

> **Updating:** `git pull`, then `docker compose up -d --build`. The `centsible-migrations` container applies any new database migrations on every start.

### Local development

Local development runs the three pieces **directly** — no Docker:

1. **PostgreSQL 18** — create the `centsible` database and apply `centsible-db/tables.sql`, then the migrations under `centsible-db/migrations` (`run-migrations.sh` automates this; re-running is safe).
2. **Backend** — in `centsible-rest` and `centsible-export`, copy `src/main/resources/application.properties.example` to `application.properties`, point `spring.datasource.*` at your DB, then run each module:
   ```bash
   ./gradlew :centsible-rest:bootRun
   ./gradlew :centsible-export:bootRun
   ```
   For local HTTP dev, set `SPRING_PROFILES_ACTIVE=` (empty), `AUTH_COOKIE_SECURE=false`, `SKIP_EMAIL_VERIFICATION=true`, and `REGISTRATION_ENABLED=true` (to create the first account).
3. **Frontend:**
   ```bash
   cd centsible-ui
   npm install
   npm run dev
   ```
   Override `NUXT_PUBLIC_API_BASE` if the API isn't at `http://localhost:8080/api`.

**Enable the git hooks (one-time, after cloning).** The repo ships a pre-commit hook in `.githooks/` that refuses to commit any `application.properties` (these hold secrets — JWT, DB credentials, API keys). Git does not enable hooks automatically, so opt in once per clone:

```bash
git config core.hooksPath .githooks
```

This is the safety net behind the gitignore rule; see [ADR-0014](docs/adr/0014-secrets-handling-precommit-hook.md) for the full secrets-handling convention.

## Module Scripts

### Frontend (`centsible-ui`)

Managed with `npm`.

- `npm run dev`: Start Nuxt development server.
- `npm run build`: Build for production.
- `npm run generate`: Static site generation.
- `npm run preview`: Preview production build.

### Backend (`centsible-rest`, `centsible-export`)

Managed with Gradle (Kotlin DSL) using a multi-module setup and **Gradle Version Catalog** (`gradle/libs.versions.toml`)
for dependency management.

- `./gradlew :centsible-rest:bootRun`: Run the REST API.
- `./gradlew :centsible-export:bootRun`: Run the export worker (requires a reachable DB).
- `./gradlew build`: Build all modules and run tests.
- `./gradlew :centsible-jooq:jooqCodegen`: Generate jOOQ classes from the database schema.
- `./gradlew :centsible-proto:generateProto`: Regenerate protobuf classes from `centsible-proto/src/main/proto/export.proto`.

## Releasing

Releases are cut with the `centsible-releaser` helper — a Node ≥ 23.6 script that runs its TypeScript entrypoint directly. From a clean `develop` checkout:

```bash
cd centsible-releaser
npm install                    # first time only
node release.mts <version>     # e.g. 0.5.0 — add --dry-run to preview
```

It aborts unless the working tree is clean, the current branch is `develop`, and no `v<version>` tag exists yet. It then, in order:

1. Creates the `release/<version>` branch.
2. Bumps the version in `build.gradle.kts` and `centsible-ui/package.json`.
3. Rolls every pending script from `centsible-db/migrations/snapshot/` into a new `centsible-db/migrations/<version>/` folder and appends `NN_SetVersion_<version>.sql`, which records the release in `system_information` (see [ADR-0013](docs/adr/0013-sql-migrations-and-jooq-codegen.md)).
4. Verifies the backend (`./gradlew build`, which runs the DB-free unit tests) and frontend (`npm run build`) builds.
5. Commits, tags `v<version>`, and pushes the branch and tag to `origin`.

Promoting `release/<version>` to `main` (the deploy branch) is a separate step. Pass `--dry-run` to print the full plan without touching git, the filesystem, or the build.

## Tests

- **Backend**: Run `./gradlew test` in the `centsible-rest` directory.
- **Frontend**: TODO: Add frontend test command (e.g., `npm test`) if tests are implemented.
- **API**: Use the collections in `centsible-bruno` with the Bruno client.

## Test Data Seeding

The `centsible-bruno/testdata/` folder is a self-contained Bruno collection that seeds a realistic dataset against a running stack (1 user, 4 accounts, 4 custom categories, 5 contacts, ~28 transactions across Feb–May 2026, 4 loans with repayments, 2 exports).

### How to run

1. Run the REST API with `REGISTRATION_ENABLED=true` and `SKIP_EMAIL_VERIFICATION=true` (see [Local development](#local-development)). Without the first, `01_register.bru` is rejected with "Registration is disabled"; without the second, it returns no session token.
2. Open `centsible-bruno/` in [Bruno](https://www.usebruno.com/).
3. Right-click the `testdata` folder and choose **Run** to execute every request top-to-bottom. Folders run in `seq` order, and files within each folder do too.

Each create-request stores the returned id in a Bruno runtime variable (e.g. `accountCheckingId`, `categoryFoodId`, `contactAliceId`) so later requests can reference it — no environment file is required.

### Seeded credentials

| Field    | Value                  |
|:---------|:-----------------------|
| username | `testuser`             |
| password | `TestPass123!`         |
| email    | `testuser@example.com` |

### Re-running

- `01_register.bru` returns 4xx if the test user already exists. That is fine — `02_login.bru` logs in with the same credentials and refreshes the `{{token}}` variable.
- Categories have a global unique-name constraint, so the custom-category creates 4xx on a second run. The seeded ones are still picked up via `categories/01_fetch_seeded.bru`.
- Accounts, contacts, transactions, loans, and exports are duplicated on each run. Reset the database for a clean slate (drop & recreate it, or `docker compose down -v` if you're running the deployment stack).

## Environment Variables

### Database

| Variable                       | Description                                                                                  | Default                              |
|:-------------------------------|:---------------------------------------------------------------------------------------------|:-------------------------------------|
| `POSTGRES_USER`                | Database username                                                                            | —                                    |
| `POSTGRES_PASSWORD`            | Database password (generate: `openssl rand -base64 32`)                                      | —                                    |
| `POSTGRES_DB`                  | Database name                                                                                | `centsible`                          |

### REST API

| Variable                            | Description                                                                                                                                          | Default                          |
|:------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------|:---------------------------------|
| `JWT_SECRET`                        | Base64-encoded JWT signing key (≥256 bits — `openssl rand -base64 64`)                                                                               | —                                |
| `JWT_EXPIRATION_MS`                 | JWT lifetime in ms                                                                                                                                   | `14400000` (4h)                  |
| `APP_BASE_URL`                      | User-facing UI origin (used in confirmation email links). MUST be `https://…` in prod                                                                | `http://localhost:3000`          |
| `SKIP_EMAIL_VERIFICATION`           | Auto-confirms new registrations (dev only — `ProductionGuard` rejects `true` under prod profile)                                                     | `false`                          |
| `SPRING_PROFILES_ACTIVE`            | Set to `prod` for public deployments to activate `ProductionGuard` invariant checks                                                                  | empty                            |
| `CORS_ALLOWED_ORIGINS`              | Comma-separated origin allowlist for the UI. Never use wildcards                                                                                     | `http://localhost:3000`          |
| `REGISTRATION_ENABLED`              | Kill switch for `POST /api/auth/register`. Flip `true` to create your account, then back to `false`                                                  | `false`                          |
| `ADMIN_ENABLED`                     | Kill switch for the admin area (`/api/admin` + the UI's Admin view). When `true`, `ADMIN_USERNAME` must be set (ProductionGuard enforces under prod)  | `false`                          |
| `ADMIN_USERNAME`                    | Username of the account granted the admin view (list/delete users, resend verification emails). Changing it requires a restart                       | empty                            |
| `AUTH_COOKIE_SECURE`                | Sets the `Secure` flag on the auth cookie. MUST be `true` in prod (ProductionGuard enforces under `prod` profile)                                    | `false`                          |
| `AUTH_COOKIE_DOMAIN`                | Cookie `Domain` attribute. Set when UI and API share a parent domain                                                                                 | empty                            |
| `SERVER_FORWARD_HEADERS_STRATEGY`   | `framework` honours `X-Forwarded-*` from a trusted proxy. Use `none` if no proxy — otherwise the rate-limit client-IP becomes spoofable              | `framework`                      |
| `JAVA_OPTS`                         | JVM options for the REST container                                                                                                                   | `-Xms128m -Xmx384m`              |

### Email (Resend)

| Variable                  | Description                          | Default |
|:--------------------------|:-------------------------------------|:--------|
| `RESEND_API_KEY`          | API key for the Resend email service | —       |
| `RESEND_FROM_EMAIL`       | "From" address on outbound mail      | —       |

### Export worker

| Variable                       | Description                                                                                  | Default                |
|:-------------------------------|:---------------------------------------------------------------------------------------------|:-----------------------|
| `EXPORT_POLL_INTERVAL_MS`      | How often the worker polls for queued jobs (ms)                                              | `2000`                 |
| `EXPORT_LEASE_TIMEOUT_SECONDS` | Worker lease TTL — jobs leased but not finished within this window are reclaimable by others | `300`                  |
| `EXPORT_RENDER_TIMEOUT_MS`     | Playwright timeout for a single PDF render — a hung render fails the job instead of stalling the worker | `60000`                |
| `EXPORT_JAVA_OPTS`             | JVM options for the export container                                                         | `-Xms256m -Xmx512m`    |

### Integrations (provider plugins — only required if storing third-party credentials)

| Variable                              | Description                                                                                           | Default      |
|:--------------------------------------|:------------------------------------------------------------------------------------------------------|:-------------|
| `INTEGRATIONS_ENCRYPTION_KEY`         | Passphrase used to derive the AES-256 key that encrypts provider credentials at rest                  | empty        |
| `INTEGRATIONS_ENCRYPTION_SALT`        | Hex string, ≥16 chars (`openssl rand -hex 16`)                                                        | empty        |
| `INTEGRATIONS_SYNC_POLL_INTERVAL_MS`  | How often the sync orchestrator polls (ms)                                                            | `300000`     |
| `INTEGRATIONS_SYNC_INTERVAL_SECONDS`  | How often a single connection is re-synced (s)                                                        | `3600`       |
| `INTEGRATIONS_BASE_URL`               | Public URL used to build OAuth redirect URIs. Required for OAuth providers. HTTPS in prod.            | empty        |
| `INTEGRATIONS_MANUAL_ENABLED`         | Toggle the built-in manual-entry provider                                                             | `true`       |

#### PayPal

Per-user PayPal credentials are entered in the UI; the operator only flips the toggle.

| Variable                                  | Description                                              | Default |
|:------------------------------------------|:---------------------------------------------------------|:--------|
| `INTEGRATIONS_PAYPAL_ENABLED`             | Show the PayPal provider in the UI                       | `false` |
| `INTEGRATIONS_PAYPAL_DEFAULT_ENVIRONMENT` | Default environment shown to users (`live` or `sandbox`) | `live`  |
| `INTEGRATIONS_PAYPAL_HTTP_TIMEOUT_MS`     | HTTP timeout for PayPal API calls                        | `15000` |

To use: each user creates a REST API app at [developer.paypal.com](https://developer.paypal.com), copies the client ID + secret into Centsible's Connect dialog, and Centsible uses the OAuth2 `client_credentials` grant to read their PayPal Transactions API. No global PayPal app or redirect URI is needed.

#### EU Banking via GoCardless Bank Account Data (formerly Nordigen)

Connects any EU/EEA bank under PSD2. The operator registers ONE app at [bankaccountdata.gocardless.com](https://bankaccountdata.gocardless.com) and shares the secrets across all users on this instance. End users only authorize bank consent through the OAuth flow.

| Variable                                                    | Description                                                             | Default      |
|:------------------------------------------------------------|:------------------------------------------------------------------------|:-------------|
| `INTEGRATIONS_BANKING_GOCARDLESS_ENABLED`                   | Show the EU banking provider in the UI                                  | `false`      |
| `INTEGRATIONS_BANKING_GOCARDLESS_SECRET_ID`                 | GoCardless BAD `secret_id` (operator-level)                             | empty        |
| `INTEGRATIONS_BANKING_GOCARDLESS_SECRET_KEY`                | GoCardless BAD `secret_key` (operator-level)                            | empty        |
| `INTEGRATIONS_BANKING_GOCARDLESS_MIN_SYNC_INTERVAL_SECONDS` | Minimum gap between syncs — respects GoCardless's 4 req/account/day cap | `21600` (6h) |

Set `INTEGRATIONS_BASE_URL=https://your.domain` and register the callback URL `https://your.domain/api/integrations/oauth/callback/banking-gocardless` in the GoCardless dashboard if required. PSD2 consent lasts 90 days, after which users must re-authorize (the UI surfaces a "Reconnect" CTA on expired connections).

### UI

| Variable               | Description                        | Default                          |
|:-----------------------|:-----------------------------------|:---------------------------------|
| `NUXT_PUBLIC_API_BASE` | Public API URL used by the browser | `http://localhost:8080/api`      |
| `NUXT_API_BASE_SSR`    | API URL used during Nuxt SSR       | `http://centsible-rest:8080/api` |

### Runtime

| Variable | Description                        | Default |
|:---------|:-----------------------------------|:--------|
| `TZ`     | Timezone applied to all containers | `UTC`   |

## License

This project is licensed under the [LICENSE](LICENSE) file found in the root directory.
