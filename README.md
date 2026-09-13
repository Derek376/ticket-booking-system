# Ticket Booking System

A reserved-seat event booking application built with Java and Spring Boot.

The backend currently provides an HTTP health endpoint and a startup test. PostgreSQL persistence, reservations, Redis locking, Kafka events, and a frontend are planned in the [development roadmap](docs/DEVELOPMENT_PLAN.md).

## Requirements

- JDK 21
- Internet access for the first build

The Maven wrapper is included, so a separate Maven installation is not required.

## Run

```sh
cd backend
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

Check [localhost:8080/actuator/health](http://localhost:8080/actuator/health). A healthy application returns HTTP 200 with a top-level `status` of `UP`. There is no homepage yet.

If port 8080 is occupied, pass `-Dspring-boot.run.arguments=--server.port=8081` to the run command.

## Test and build

From `backend/`:

```sh
./mvnw verify
```

The test starts the application on a random port and checks its HTTP health response. Run the packaged application with:

```sh
java -jar target/ticket-booking-0.0.1-SNAPSHOT.jar
```
