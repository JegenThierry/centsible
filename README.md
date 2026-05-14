# Budget Planner

A full-stack application for comprehensive budget management. **NOTE: This project is in its early development stages,
and bugs may occur. If you find any issues, please feel free to create an issue.**

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

The project uses an `.env` file at the root for configuration. Copy `.env.example` to `.env` and update the values as needed.

```env
# ===== Database =====
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change_me_super_strong_password
POSTGRES_DB=budget_planner
DB_HOST_PORT=5432

# ===== REST API =====
REST_HOST_PORT=8080
JWT_SECRET=your-secret-here
JWT_EXPIRATION_MS=86400000
APP_BASE_URL=http://localhost:8080
SKIP_EMAIL_VERIFICATION=true
JAVA_OPTS='-Xms128m -Xmx384m'

# ===== Resend (email) =====
RESEND_API_KEY=re_123456789
RESEND_FROM_EMAIL=onboarding@resend.dev

# ===== Export Service =====
EXPORT_HOST_PORT=8081
EXPORT_POLL_INTERVAL_MS=2000
EXPORT_LEASE_TIMEOUT_SECONDS=300

# ===== UI =====
UI_HOST_PORT=3000
NUXT_PUBLIC_API_BASE=http://localhost:8080/api
NUXT_API_BASE_SSR=http://budget-planner-rest:8080/api

# ===== Runtime =====
TZ=UTC
```

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

- PostgreSQL on port `5432` (or as configured in `DB_HOST_PORT`)
- REST API on port `8080` (or as configured in `REST_HOST_PORT`)
- Export worker on port `8081` (or as configured in `EXPORT_HOST_PORT`) — actuator only; the worker has no HTTP API of its own.
- UI on port `3000` (or as configured in `UI_HOST_PORT`)

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

## Environment Variables

| Variable                       | Description                                                                                  | Default                      |
|:-------------------------------|:---------------------------------------------------------------------------------------------|:-----------------------------|
| `POSTGRES_USER`                | Database username                                                                            | -                            |
| `POSTGRES_PASSWORD`            | Database password                                                                            | -                            |
| `POSTGRES_DB`                  | Database name                                                                                | `budget_planner`             |
| `DB_HOST_PORT`                 | Port for PostgreSQL                                                                          | `5432`                       |
| `JWT_SECRET`                   | Secret key for JWT signing                                                                   | -                            |
| `JWT_EXPIRATION_MS`            | JWT token expiration in ms                                                                   | `86400000` (24h)             |
| `REST_HOST_PORT`               | Port for REST API                                                                            | `8080`                       |
| `JAVA_OPTS`                    | JVM options for the REST container                                                           | `-Xms128m -Xmx384m`          |
| `RESEND_API_KEY`               | API key for Resend email service                                                             | -                            |
| `RESEND_FROM_EMAIL`            | Email address to send emails from                                                            | -                            |
| `APP_BASE_URL`                 | Base URL for the application                                                                 | `http://localhost:8080`      |
| `SKIP_EMAIL_VERIFICATION`      | Skip email verification during registration                                                  | `false`                      |
| `EXPORT_HOST_PORT`             | Port for the export worker's actuator                                                        | `8081`                       |
| `EXPORT_POLL_INTERVAL_MS`      | How often the export worker polls for queued jobs (ms)                                       | `2000`                       |
| `EXPORT_LEASE_TIMEOUT_SECONDS` | Worker lease TTL — jobs leased but not finished within this window are reclaimable by others | `300`                        |
| `EXPORT_JAVA_OPTS`             | JVM options for the export container                                                         | `-Xms256m -Xmx512m`          |
| `UI_HOST_PORT`                 | Port for UI                                                                                  | `3000`                       |
| `NUXT_PUBLIC_API_BASE`         | Public API URL for the frontend                                                              | `http://localhost:8080/api`  |
| `NUXT_API_BASE_SSR`            | API URL for SSR in Nuxt                                                                      | -                            |
| `TZ`                           | Timezone applied to all containers                                                           | `UTC`                        |

## License

This project is licensed under the [LICENSE](LICENSE) file found in the root directory.
