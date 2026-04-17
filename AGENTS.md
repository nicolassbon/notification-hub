# Notification Hub - Agent Instructions

## Repository Stack
- **Language & Framework**: Java 21, Spring Boot 3.5+, Maven
- **Database**: PostgreSQL (Production/Docker) / MySQL 8.0 (CI). Unit/Integration tests use **H2** in-memory database.
- **Architecture**: Classic layered (`controller` -> `service` -> `repository`), utilizing `dto`, `entity`, and `mapper` packages.
- **Tooling**: Uses **Lombok** and **MapStruct** for DTO mappings.

## Execution & Testing
Always use the Maven Wrapper (`./mvnw` on Linux/Mac, `mvnw.cmd` on Windows):

- **Run Local Server**: `./mvnw spring-boot:run`
- **Run All Tests**: `./mvnw test`
- **Run Specific Test**: `./mvnw test -Dtest=ClassNameTest`
- **Verify & Coverage**: `./mvnw verify` (Generates JaCoCo report at `target/site/jacoco/jacoco.xml`)
- **Package (skip tests)**: `./mvnw clean package -DskipTests`
- **Run with Docker**: `docker-compose up -d` (runs the app and a PostgreSQL container)

## Architecture & Code Constraints
- **Coverage Exclusions**: JaCoCo ignores `com/notificationhub/dto/**`, `com/notificationhub/entity/**`, `com/notificationhub/enums/**`, `com/notificationhub/config/**`, and `com/notificationhub/mapper/**`. Do not write trivial tests just to cover simple data classes.
- **Message Delivery Transactionality**: The system enforces that messages are *only* saved to the database if at least one delivery attempt (Telegram/Discord) succeeds. Preserve this strict rule during any refactoring.
- **Pagination & N+1**: The project explicitly optimizes lazy loading for `OneToMany` relationships in paginated endpoints. Be highly vigilant against introducing N+1 query bugs when modifying JPA Repositories or Entities. Use queries without `FETCH` for pagination, then lazy-load selectively.
- **Rate Limiting**: Daily message limits are enforced and cached using Spring Cache (Caffeine). Ensure any new notification-sending endpoints integrate with existing rate-limiting mechanisms and pessimistic locking to avoid race conditions.

## CI/CD Pipeline
- **Validation**: Pushes to `main`/`develop` and PRs to `main` trigger tests (using MySQL 8.0) and **SonarCloud** quality gate analysis.
- **Deployment**: Code merged to `main` is packaged into a `.jar` and automatically deployed to Railway (and optionally Azure Web Apps). Always ensure the build passes `verify` locally before pushing.
