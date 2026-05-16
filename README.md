# Centsible

A clean, self-hosted budget tracker — multiple accounts, smart categories, monthly budgets, and dashboards that actually help you decide. **NOTE: This project is in its early development stages, and bugs may occur. If you find any issues, please feel free to create an issue.**

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

> **A note on naming.** The product is **Centsible**, but the modules, Kotlin packages (`beer.thierry.budgetplanner.*`), Docker service names, the `budget_planner` database, and the jOOQ-generated code under `beer.thierry.jooq.generated` still use the original `budget-planner` identifier. These are internal infrastructure names — renaming them would churn generated sources, Spring `@ComponentScan` lists, Docker Compose references, environment variables, and git history with no user-facing benefit. Treat `budget-planner-*` as the codebase's legacy skin; **Centsible** is what users see.

- **`budget-planner-api`**: Shared domain models, DTOs, and interfaces.
- **`budget-planner-bruno`**: API request collections for the [Bruno](https://www.usebruno.com/) API client.
- **`budget-planner-core`**: Core business logic and service implementations.
- **`budget-planner-db`**: Database schema, migrations, and initialization scripts.
- **`budget-planner-export`**: Standalone Spring Boot worker that picks up queued export jobs, renders PDFs via Pebble templates + Playwright, and runs post-processors (e.g. email delivery).
- **`budget-planner-jooq`**: Data access layer powered by jOOQ.
- **`budget-planner-proto`**: Protobuf schema (`export.proto`) shared between `rest` and `export` to describe the export job payload.
- **`budget-planner-rest`**: Spring Boot entry point and REST API controllers.
- **`budget-planner-ui`**: Nuxt.js frontend application.

### N-Layer Architecture (Backend)

The backend is decomposed into several modules, each representing a specific layer in the architecture. Dependency flow is strictly unidirectional (from outer layers to inner layers):

1.  **Presentation Layer (`budget-planner-rest`)**:
    - The entry point of the application.
    - Contains Spring Boot configuration, Security setup, and REST Controllers.
    - Responsible for handling HTTP requests, input validation, and mapping to/from DTOs.
    - Builds protobuf export payloads (via `budget-planner-proto`) and enqueues jobs that the export worker consumes.
    - **Depends on**: `budget-planner-core`, `budget-planner-api`, `budget-planner-proto`.

2.  **Business Layer (`budget-planner-core`)**:
    - Contains the "heart" of the application: services, facades, and business rules.
    - Orchestrates transactions and coordinates data flow between the API and Persistence layers.
    - **Depends on**: `budget-planner-jooq`, `budget-planner-api`.

3.  **Data Access Layer (`budget-planner-jooq`)**:
    - Handles all database interactions.
    - Includes jOOQ-generated classes and custom repository implementations.
    - Encapsulates SQL logic and provides a clean interface for the Business layer.
    - **Depends on**: `budget-planner-api`.

4.  **Shared Layer (`budget-planner-api`)**:
    - A lightweight module containing common DTOs, interfaces, constants, and exceptions.
    - Used as a bridge for communication between all other modules.
    - **Depends on**: None.

In addition, two cross-cutting modules support the export pipeline:

- **`budget-planner-proto`**: Pure protobuf module — generates Java classes from `src/main/proto/export.proto`. Consumed by both `rest` (producer) and `export` (consumer) so the wire format stays in sync.
- **`budget-planner-export`**: A separate Spring Boot service that polls the `export_jobs` table, reads the protobuf payload, renders a PDF (Pebble + Playwright), and runs any registered post-processors. It runs as its own container alongside `rest` and shares the `jooq` + `api` modules.

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

- [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/) installed on your system.
- (Optional) [JDK 21](https://adoptium.net/) if running the backend locally without Docker.
- (Optional) [Node.js](https://nodejs.org/) (v20+) if running the frontend locally without Docker.

### Configuration

Copy `.env.example` to `.env` and adjust values. The full set of variables (with annotations) is documented inline there; see also the **Environment Variables** section below for descriptions and defaults.

Defaults are tuned for a **production-shaped** deployment (registration disabled, `Secure` cookies, `SPRING_PROFILES_ACTIVE=prod`). For local HTTP dev, override the relevant variables — typically by setting `SKIP_EMAIL_VERIFICATION=true`, `AUTH_COOKIE_SECURE=false`, `SPRING_PROFILES_ACTIVE=` (empty), and `REGISTRATION_ENABLED=true` when you need to create an account.

### Installation and Deployment

To run the entire stack using Docker Compose:

```bash
docker compose up --build
```

For **development with auto-rebuild on file changes**, use watch mode:

```bash
docker compose up --watch
```

This monitors your source files and automatically rebuilds or syncs containers when changes are detected.

This will start:

- PostgreSQL on `127.0.0.1:5432` (configurable via `DB_HOST_PORT`; bound to localhost only — never published externally)
- REST API on port `8080` (or as configured in `REST_HOST_PORT`)
- Export worker — internal only, not published to the host
- UI on port `3000` (or as configured in `UI_HOST_PORT`)

### Production deployment behind a reverse proxy

The base `docker-compose.yml` runs the UI in `nuxt dev` mode with hot-reload and publishes ports to the host — suitable for local work, **not** for a public deployment. For production, apply the prod overlay:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

The overlay:

- builds the UI from the `release` stage (compiled Nuxt output, no `nuxt dev`),
- drops the UI source bind-mount and all `develop:` watch blocks,
- unpublishes the REST, UI, and Postgres ports — the stack is reachable only over Docker networks.

A reverse proxy (nginx-proxy-manager, Caddy, Traefik, …) must terminate TLS and forward requests to `budget-planner-ui:3000` and `budget-planner-rest:8080`. The overlay attaches both services to an external `proxy-net` network — set `PROXY_NETWORK` in `.env` to the Docker network name of your proxy stack (default `nginx-proxy-manager_default`; check `docker network ls`).

Pick one topology in your proxy config:

| Topology | Proxy routing | `.env` values |
|:---|:---|:---|
| Two subdomains | `app.example.com → budget-planner-ui:3000`<br/>`api.example.com → budget-planner-rest:8080` | `APP_BASE_URL=https://app.example.com`<br/>`CORS_ALLOWED_ORIGINS=https://app.example.com`<br/>`AUTH_COOKIE_DOMAIN=.example.com`<br/>`NUXT_PUBLIC_API_BASE=https://api.example.com/api` |
| Single host, path-based | `example.com/api/* → budget-planner-rest:8080`<br/>`example.com/* → budget-planner-ui:3000` | `APP_BASE_URL=https://example.com`<br/>`CORS_ALLOWED_ORIGINS=https://example.com`<br/>`AUTH_COOKIE_DOMAIN=`<br/>`NUXT_PUBLIC_API_BASE=/api` |

Either way, `AUTH_COOKIE_SECURE=true` and `SPRING_PROFILES_ACTIVE=prod` must be set (ProductionGuard enforces this).

## Module Scripts

### Frontend (`budget-planner-ui`)

Managed with `npm`.

- `npm run dev`: Start Nuxt development server.
- `npm run build`: Build for production.
- `npm run generate`: Static site generation.
- `npm run preview`: Preview production build.

### Backend (`budget-planner-rest`, `budget-planner-export`)

Managed with Gradle (Kotlin DSL) using a multi-module setup and **Gradle Version Catalog** (`gradle/libs.versions.toml`)
for dependency management.

- `./gradlew :budget-planner-rest:bootRun`: Run the REST API.
- `./gradlew :budget-planner-export:bootRun`: Run the export worker (requires a reachable DB).
- `./gradlew build`: Build all modules and run tests.
- `./gradlew :budget-planner-jooq:jooqCodegen`: Generate jOOQ classes from the database schema.
- `./gradlew :budget-planner-proto:generateProto`: Regenerate protobuf classes from `budget-planner-proto/src/main/proto/export.proto`.

## Tests

- **Backend**: Run `./gradlew test` in the `budget-planner-rest` directory.
- **Frontend**: TODO: Add frontend test command (e.g., `npm test`) if tests are implemented.
- **API**: Use the collections in `budget-planner-bruno` with the Bruno client.

## Test Data Seeding

The `budget-planner-bruno/testdata/` folder is a self-contained Bruno collection that seeds a realistic dataset against a running stack (1 user, 4 accounts, 4 custom categories, 5 contacts, ~28 transactions across Feb–May 2026, 4 loans with repayments, 2 exports).

### How to run

1. In `.env`, set `REGISTRATION_ENABLED=true` and `SKIP_EMAIL_VERIFICATION=true`. Without the first, `01_register.bru` is rejected with "Registration is disabled"; without the second, it returns no session token. Then start the stack:
   ```bash
   docker compose up --build
   ```
2. Open `budget-planner-bruno/` in [Bruno](https://www.usebruno.com/).
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
- Accounts, contacts, transactions, loans, and exports are duplicated on each run. Reset the DB (e.g. `docker compose down -v`) for a clean slate.

## Environment Variables

### Database

| Variable                       | Description                                                                                  | Default                              |
|:-------------------------------|:---------------------------------------------------------------------------------------------|:-------------------------------------|
| `POSTGRES_USER`                | Database username                                                                            | —                                    |
| `POSTGRES_PASSWORD`            | Database password (generate: `openssl rand -base64 32`)                                      | —                                    |
| `POSTGRES_DB`                  | Database name                                                                                | `budget_planner`                     |
| `DB_HOST_PORT`                 | Host-side port for PostgreSQL. Bound to `127.0.0.1` only — never published externally        | `5432`                               |

### REST API

| Variable                            | Description                                                                                                                                          | Default                          |
|:------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------|:---------------------------------|
| `REST_HOST_PORT`                    | Host-side port for the REST API                                                                                                                      | `8080`                           |
| `JWT_SECRET`                        | Base64-encoded JWT signing key (≥256 bits — `openssl rand -base64 64`)                                                                               | —                                |
| `JWT_EXPIRATION_MS`                 | JWT lifetime in ms                                                                                                                                   | `14400000` (4h)                  |
| `APP_BASE_URL`                      | User-facing UI origin (used in confirmation email links). MUST be `https://…` in prod                                                                | `http://localhost:3000`          |
| `SKIP_EMAIL_VERIFICATION`           | Auto-confirms new registrations (dev only — `ProductionGuard` rejects `true` under prod profile)                                                     | `false`                          |
| `SPRING_PROFILES_ACTIVE`            | Set to `prod` for public deployments to activate `ProductionGuard` invariant checks                                                                  | empty                            |
| `CORS_ALLOWED_ORIGINS`              | Comma-separated origin allowlist for the UI. Never use wildcards                                                                                     | `http://localhost:3000`          |
| `REGISTRATION_ENABLED`              | Kill switch for `POST /api/auth/register`. Flip `true` to create your account, then back to `false`                                                  | `false`                          |
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
| `EXPORT_JAVA_OPTS`             | JVM options for the export container                                                         | `-Xms256m -Xmx512m`    |

### Integrations (provider plugins — only required if storing third-party credentials)

| Variable                              | Description                                                                                           | Default      |
|:--------------------------------------|:------------------------------------------------------------------------------------------------------|:-------------|
| `INTEGRATIONS_ENCRYPTION_KEY`         | Passphrase used to derive the AES-256 key that encrypts provider credentials at rest                  | empty        |
| `INTEGRATIONS_ENCRYPTION_SALT`        | Hex string, ≥16 chars (`openssl rand -hex 16`)                                                        | empty        |
| `INTEGRATIONS_SYNC_POLL_INTERVAL_MS`  | How often the sync orchestrator polls (ms)                                                            | `300000`     |
| `INTEGRATIONS_SYNC_INTERVAL_SECONDS`  | How often a single connection is re-synced (s)                                                        | `3600`       |

### UI

| Variable               | Description                       | Default                            |
|:-----------------------|:----------------------------------|:-----------------------------------|
| `UI_HOST_PORT`         | Host-side port for the UI         | `3000`                             |
| `NUXT_PUBLIC_API_BASE` | Public API URL used by the browser| `http://localhost:8080/api`        |
| `NUXT_API_BASE_SSR`    | API URL used during Nuxt SSR      | `http://budget-planner-rest:8080/api` |

### Runtime

| Variable | Description                        | Default |
|:---------|:-----------------------------------|:--------|
| `TZ`     | Timezone applied to all containers | `UTC`   |

## License

This project is licensed under the [LICENSE](LICENSE) file found in the root directory.
