# Budget Planner

A full-stack application for comprehensive budget management.

## Project Structure

The repository is divided into the following modules:

- **`budget-planner-ui`**: Frontend application built with Nuxt 4 (Vue 3, Pinia, Tailwind CSS).
- **`budget-planner-rest`**: Backend REST API built with Spring Boot 4 and Kotlin, using jOOQ for database interactions.
- **`budget-planner-db`**: Database schema, migrations, and initialization SQL scripts for PostgreSQL.
- **`budget-planner-bruno`**: API Request collections for the [Bruno](https://www.usebruno.com/) API client.

## Tech Stack

| Component         | Technology                                  |
|:------------------|:--------------------------------------------|
| **Frontend**      | Nuxt 4, Vue 3, Pinia, Tailwind CSS, Nuxt UI |
| **Backend**       | Spring Boot 4, Kotlin, JDK 21               |
| **Database**      | PostgreSQL 18.3, jOOQ                       |
| **Orchestration** | Docker, Docker Compose                      |
| **API Testing**   | Bruno                                       |

## Getting Started

### Prerequisites

- [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/) installed on your system.
- (Optional) [JDK 21](https://adoptium.net/) if running the backend locally without Docker.
- (Optional) [Node.js](https://nodejs.org/) (v20+) if running the frontend locally without Docker.

### Configuration

The project uses an `.env` file at the root for configuration. A sample `.env` file is expected with the following
variables:

```env
# Database
POSTGRES_USER=<pg_user>
POSTGRES_PASSWORD=<pg_password>
POSTGRES_DB=budget_planner
DB_HOST_PORT=5432

# Rest
JWT_SECRET=your_jwt_secret_here
JWT_EXPIRATION_MS=86400000
REST_HOST_PORT=8080

# Ui
UI_HOST_PORT=3000
NUXT_PUBLIC_API_BASE=http://localhost:8080/api
NUXT_API_BASE_SSR=http://budget_planner_rest:8080/api
```

### Installation and Deployment

To run the entire stack using Docker Compose:

```bash
docker compose up --build
```

This will start:

- PostgreSQL on port `5432` (or as configured in `DB_HOST_PORT`)
- REST API on port `8080` (or as configured in `REST_HOST_PORT`)
- UI on port `3000` (or as configured in `UI_HOST_PORT`)

## Module Scripts

### Frontend (`budget-planner-ui`)

Managed with `npm`.

- `npm run dev`: Start Nuxt development server.
- `npm run build`: Build for production.
- `npm run generate`: Static site generation.
- `npm run preview`: Preview production build.

### Backend (`budget-planner-rest`)

Managed with Gradle (Kotlin DSL).

- `./gradlew bootRun`: Run the Spring Boot application.
- `./gradlew build`: Build the project and run tests.
- `./gradlew jooqCodegen`: Generate jOOQ classes from the database schema.
- TODO: Add more specific backend scripts if applicable.

## Tests

- **Backend**: Run `./gradlew test` in the `budget-planner-rest` directory.
- **Frontend**: TODO: Add frontend test command (e.g., `npm test`) if tests are implemented.
- **API**: Use the collections in `budget-planner-bruno` with the Bruno client.

## Environment Variables

| Variable               | Description                     | Default                     |
|:-----------------------|:--------------------------------|:----------------------------|
| `POSTGRES_USER`        | Database username               | -                           |
| `POSTGRES_PASSWORD`    | Database password               | -                           |
| `POSTGRES_DB`          | Database name                   | `budget_planner`            |
| `JWT_SECRET`           | Secret key for JWT signing      | -                           |
| `JWT_EXPIRATION_MS`    | JWT token expiration in ms      | `86400000` (24h)            |
| `NUXT_PUBLIC_API_BASE` | Public API URL for the frontend | `http://localhost:8080/api` |

## License

This project is licensed under the [LICENSE](LICENSE) file found in the root directory.
