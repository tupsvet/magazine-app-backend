# magazines-backend

Backend for the Magazines Catalog application. Built with Ktor on Kotlin 1.9 / JVM 17.

## Stack

- Ktor 2.3 (Netty engine, content negotiation, kotlinx-serialization, auth + JWT, status pages, call logging, CORS)
- Exposed 0.45 (core / dao / jdbc / java-time) on PostgreSQL
- HikariCP connection pool
- Flyway for schema migrations
- Koin for DI
- Firebase Admin SDK for token verification
- Logback for logging

## Project layout

```
src/main/kotlin/com/magazines/
├── Application.kt         # entry point (embeddedServer + Netty)
├── config/                # application configuration
├── plugins/               # Ktor feature installers
├── db/tables/             # Exposed table definitions
├── domain/
│   ├── model/             # domain entities
│   └── exception/         # domain exceptions
├── data/
│   ├── dto/               # request/response DTOs
│   └── repository/        # repository implementations
├── service/               # business services
├── routes/                # HTTP route handlers
└── util/                  # shared helpers
```

## Configuration

Settings live in `src/main/resources/application.conf` (HOCON) and read the
following environment variables (with sensible local fallbacks):

| Variable                    | Default                                                  |
|-----------------------------|----------------------------------------------------------|
| `SERVER_PORT`               | `8080`                                                   |
| `DATABASE_URL`              | `jdbc:postgresql://localhost:5432/magazines_catalog`     |
| `DATABASE_USER`             | `app`                                                    |
| `DATABASE_PASSWORD`         | `app_password`                                           |
| `STORAGE_PATH`              | `./storage`                                              |
| `FIREBASE_CREDENTIALS_PATH` | `src/main/resources/firebase-service-account.json`       |

A starter file is provided in `.env.example`.

## Running locally

1. Start the supporting services (PostgreSQL + pgAdmin):

   ```sh
   docker compose up -d
   ```

2. Build and run the server:

   ```sh
   ./gradlew run
   ```

3. Verify it's up:

   ```sh
   curl http://localhost:8080/
   # → Magazines Catalog API v1.0
   ```

## Useful tasks

| Task                | Description                  |
|---------------------|------------------------------|
| `./gradlew build`   | Compile + run tests          |
| `./gradlew run`     | Run the server               |
| `./gradlew test`    | Run tests only               |
