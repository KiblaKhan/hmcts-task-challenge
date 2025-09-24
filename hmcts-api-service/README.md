# HMCTS API SERVICE - HMCTS CHALLENGE (Backend)

This repository showcases **API-first design**, **hexagonal architecture**, lifecycle **State** pattern, list ordering **Strategy**, **BDD** tests, and a deployment footprint (Docker Compose, Helm, Terraform). It’s intentionally lean for a coding challenge while highlighting how I’d harden it for production.

> **Note on Docker & tests:** Docker/Compose wasn’t tested locally on my machine. To keep local runs simple, **integration tests (Testcontainers) and BDD tests (Cucumber)** are **disabled by default**. You can opt in via environment variables (see **Testing**).

---

## Tech stack

- **Java 21**, **Spring Boot 3.3** (Web, Validation, Data JPA, Actuator)
- **H2** (quickstart), **Postgres** (via Docker Compose), **Redis** (optional idempotency backend in Compose)
- **OpenAPI 3.1** (Swagger UI), **Flyway**, **Testcontainers**, **Cucumber** (BDD)

---

## Run (quickstart — no installs, no auth)

### Maven

```bash
cd api
./mvnw spring-boot:run
```

### Gradle (wrapper)

```bash
cd api
./gradlew bootRun          # macOS/Linux
# .\gradlew.bat bootRun   # Windows PowerShell
```

- API: <http://localhost:8080>  
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>  
- Health: <http://localhost:8080/actuator/health>

### Smoke tests

```bash
# Create (201 + Location)
curl.exe --% -X POST http://localhost:8080/tasks -H "Content-Type: application/json" -H "Idempotency-Key: demo-1" --data-binary "{""title"":""Pay fine"",""description"":""Court fee""}"

# Get (replace {id} with valid id)
curl.exe -s http://localhost:8080/tasks/{id} | ConvertFrom-Json | Format-Table

# Validation error (422, RFC 7807)
curl.exe --% -X POST http://localhost:8080/tasks -H "Content-Type: application/json" -H "Idempotency-Key: demo-2" --data-binary "{""description"":""Court fee""}"
```

---

## Run with Docker Compose (API + Postgres + Redis)

> **Heads-up:** I did **not** test Compose locally (no Docker installed). Config is included for reviewers who do have Docker. Given that, **integration and BDD tests are not run by default**.

```bash
docker compose up --build
# API http://localhost:8080
```

Compose spins up Postgres and Redis; the API reads profile/connection settings from env.

---

## API

OpenAPI: [`openapi.yaml`](./openapi.yaml)

Key endpoints:

- `POST /tasks` → **201 Created** + `Location: /tasks/{id}`  
  Supports `Idempotency-Key` (best-effort duplicate detection → **409** on key reuse).
- `GET /tasks{?sort}` (`sort=dueDate|status`) → returns a **plain array** in quickstart.
- `GET /tasks/{id}` → **200** or **404**
- `PUT /tasks/{id}/status` → **200** (state machine: `OPEN → IN_PROGRESS → DONE`)
- `DELETE /tasks/{id}` → **204** or **404**

### Error format (RFC 7807)

The API returns `application/problem+json`, e.g.:

```json
{
  "title": "Validation failed",
  "status": 422,
  "detail": "One or more fields are invalid.",
  "instance": "/tasks",
  "errors": [
    { "field": "title", "message": "must not be blank" }
  ]
}
```

Common statuses: **400** (bad params), **404** (not found), **409** (conflict), **422** (body validation).

---

## Architecture

- **Hexagonal**: `domain` (entities, rules), `application` (use cases), `ports` (interfaces), `adapters` (web, persistence, idempotency).
- **State pattern**: domain enforces legal status transitions.
- **Strategy pattern**: pluggable task listing order (`dueDate`, `status`).

---

## Configuration

See [`api/src/main/resources/application.yml`](api/src/main/resources/application.yml) (quickstart) and `application-postgres.yml` (Postgres profile).

- **Quickstart (default):** H2 in-memory DB, Flyway migrations.
- **Postgres profile:** `SPRING_PROFILES_ACTIVE=postgres` (Compose sets this).
- **Idempotency backend:**  
  - Default: JPA table (`idempotency.backend` unset or `jpa`)  
  - Redis: set `idempotency.backend=redis` and `redis.url=redis://host:6379`

---

## Testing

### Postman

- See [`docs/postman`](./docs/postman) Postman collection available for import.

### Defaults

- **Unit & MVC tests** run by default.
- **Integration (Testcontainers)** and **BDD (Cucumber)** are **disabled by default** (no Docker requirement). Enable them with env vars below.

### Maven run tests

```bash
cd api
# Unit & MVC only (default)
./mvnw test

# Integration tests (requires Docker running)
RUN_TESTCONTAINERS=true ./mvnw test

# BDD / Cucumber (against a running app on localhost:8080)
RUN_BDD=true ./mvnw test

# Both (integration + BDD)
RUN_TESTCONTAINERS=true RUN_BDD=true ./mvnw test
```

### Gradle (wrapper) run tests

```bash
cd api
# Unit & MVC only (default)
./gradlew test                 # or .\gradlew.bat test

# Integration tests (requires Docker)
RUN_TESTCONTAINERS=true ./gradlew integrationTest
# Windows PowerShell: $env:RUN_TESTCONTAINERS="true"; .\gradlew.bat integrationTest

# BDD / Cucumber (against a running app on localhost:8080)
RUN_BDD=true ./gradlew functionalTest
# Windows PowerShell: $env:RUN_BDD="true"; .\gradlew.bat functionalTest

# Both
RUN_TESTCONTAINERS=true RUN_BDD=true ./gradlew check
```

> Tests use `@EnabledIfEnvironmentVariable` so they **auto-skip** unless the flag is set. If you prefer POM-level control, you can add Surefire/Failsafe profiles to include/exclude tags.

---

## Observability

- Spring **Actuator**: `/actuator/health`, `/actuator/metrics`
- (Roadmap) Structured JSON logs, correlation IDs, metrics dashboards.

---

## Future improvements & recommendations

### Reliability & correctness

- **Testing:** Improve unit tests, test coverage, integration test with docker.
- **Idempotency (full replay):** persist `{status, body, Location, created_at, expires_at}` and **replay** on same fingerprint (current build does best-effort 409 only).
- **Pagination envelope:** return `{ data, meta { page, page_size, total_items, total_pages }, links { self,next,prev } }` and **Link** headers.
- **ETag / Conditional GET** for reads (`ETag`, `If-None-Match`).

### Security

- **JWT bearer** (resource server) with JWKS; scopes/roles per endpoint.
- **Input hardening** and **least privilege**: strict validation, sanitize logs, deny by default.
- **Rate limiting** with `RateLimit-*` headers; protect mutating endpoints.
- **CORS** for intended origins; CSRF off for stateless APIs.

### Logging & observability

- **Structured JSON logs** (logback encoder): include `traceId`, `spanId`, `http.*` fields.
- **Correlation IDs**: accept `X-Request-Id` (MDC); generate if missing.
- **Audit events** for status transitions; metrics via Micrometer; tracing via OpenTelemetry.

### Scalability & performance

- Stateless app instances; idempotency in Redis/DB.
- DB **indexes** on `status`, `dueAt`; verify query plans under load.
- Sensible paging caps; circuit breakers for future outbound calls.
- Consider async pipelines if long-running operations appear.

### SOLID / SRP

- **Controllers thin (I/O only)**; no domain logic in controllers.
- **One endpoint → one use case call**; controllers orchestrate I/O and delegate to application layer.
- Dedicated **DTO mappers** (MapStruct or small manual mappers).
- **Interface segregation** in ports (separate read/write if needed).
- **Open-closed**: add new sort orders via additional `TaskListingStrategy` beans.

### Data & migrations

- **Flyway** as the default (already enabled): versioned SQL, repeatables.
- **Soft delete & audit trail** if retention is required.

### Developer experience

- **Pre-commit**: format, lint, license headers.
- **CI**: build, tests (unit/BDD), dependency scan, container build, SBOM.
- Profiles: `quickstart` (H2, open), `postgres`, `secure` (JWT + Postgres + full idempotency replay).

---

## File map (high level)

- `api/src/main/java/uk/gov/hmcts/tasks/domain` – entities, value objects, state machine  
- `api/src/main/java/uk/gov/hmcts/tasks/application` – use cases, ports, strategies  
- `api/src/main/java/uk/gov/hmcts/tasks/infrastructure` – web/persistence/idempotency adapters  
- `openapi.yaml` – API contract  
- `docker-compose.yml`, `helm/api/*`, `infra/terraform/*` – deployment scaffolding  
- `docs/postman/HMCTS-API-SERVICE.postman_collection.json` – Postman tests
