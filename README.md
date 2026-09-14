# Ticket Booking System

A reserved-seat event booking application built with Java and Spring Boot.

The backend connects to PostgreSQL and applies versioned database migrations at startup. Integration tests cover database access, constraints, transaction rollback, and the HTTP health endpoint. Catalog APIs, reservations, Redis locking, Kafka events, and a frontend are planned in the [development roadmap](docs/DEVELOPMENT_PLAN.md).

## Requirements

- JDK 21
- Docker with Compose (Docker Desktop on macOS and Windows)
- Internet access for the first build and container downloads

The Maven wrapper is included, so a separate Maven installation is not required.

## Run

Start Docker, then run these commands from the repository root:

```sh
docker compose up --wait
cd backend
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

Flyway creates the `venues` table and records the applied migration in `flyway_schema_history`. Later startups validate the migration history and apply only new migrations. Add a new versioned SQL file for schema changes instead of editing an applied migration.

Check [localhost:8080/actuator/health](http://localhost:8080/actuator/health). A healthy application returns HTTP 200 with a top-level `status` of `UP`, including the database connection check. There is no homepage yet.

If port 8080 is occupied, pass `-Dspring-boot.run.arguments=--server.port=8081` to the run command.

Stop the application with Ctrl+C. Run `docker compose down` from the repository root to stop PostgreSQL. Database files remain in the named `postgres_data` volume and are reused on the next startup.

## Database configuration

Compose and the backend share these local defaults:

| Environment variable | Default |
| --- | --- |
| `DB_PORT` | `5432` |
| `DB_NAME` | `ticket_booking` |
| `DB_USERNAME` | `ticket_booking` |
| `DB_PASSWORD` | `ticket_booking_local` |

These credentials are for local development. PostgreSQL is exposed only on `127.0.0.1`. Export overrides in the same shell before starting Compose and the backend; for example, `export DB_PORT=5433` if port 5432 is occupied. Spring Boot does not automatically read Compose's `.env` file.

Set `DB_URL` to a full JDBC URL to connect the backend to a different PostgreSQL server. It overrides the URL built from the local port and database name. Set `DB_USERNAME` and `DB_PASSWORD` for that server as well.

PostgreSQL's initial database and credentials are created only when its data volume is empty. Changing the environment variables does not update users or passwords in an existing database.

## Test and build

With Docker running, execute this from `backend/`:

```sh
./mvnw verify
```

Testcontainers creates a disposable PostgreSQL 18.6 database on a random port. Spring Boot's service connection supplies its credentials to the application and Flyway, so tests do not use the development database or require Compose to be running. Containers are removed after the test application context closes. Tests fail if Docker is unavailable.

The tests check fresh migrations, migration re-runs, venue inserts and reads, a database constraint, transaction rollback, and HTTP health. GitHub Actions runs the same verification command on pushes and pull requests using Java 21 and Docker.

With the development database running, start the packaged application with:

```sh
java -jar target/ticket-booking-0.0.1-SNAPSHOT.jar
```
