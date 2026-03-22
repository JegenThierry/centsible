# Budget Planner

A full-stack application for budget management.

## Project Structure

The repository is divided into the following modules:

* **`budget-planner-ui`**: Frontend application built with Nuxt.
* **`budget-planner-rest`**: Backend REST API built with Spring Boot.
* **`budget-planner-db`**: Database schema and initialization SQL scripts for PostgreSQL.

## Tech Stack

| Component | Technology |
| :--- | :--- |
| **UI** | Nuxt |
| **REST API** | Spring Boot |
| **Database** | PostgreSQL |
| **Orchestration** | Docker |

## Getting Started

### Prerequisites

* Docker and Docker Compose installed on your system.

### Installation and Deployment

To run the application on your system for development use:

```bash
docker compose up --build
```