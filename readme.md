# Resource Lending API

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 3.4.4](https://img.shields.io/badge/Spring_Boot-3.4.4-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Tests](https://img.shields.io/badge/tests-20_passing-brightgreen)
![Modernisation](https://img.shields.io/badge/modernisation-phase_3_complete-blue)

A Java and Spring Boot REST API being re-engineered into a production-oriented platform for lending organisational resources. It currently manages library-style customers, catalogue exemplars and loans while its staged roadmap expands the domain to equipment, reservations, policies, auditability and concurrency-safe workflows.

> Originally developed as a university library management project and re-engineered as a production-oriented resource lending API, with a redesigned domain, security model, transactional workflows, database migrations, automated testing and containerised delivery.

That statement describes the target journey. Completed work and planned capabilities are deliberately separated below so the repository never presents roadmap items as shipped features.

## Why this project

Lending systems look simple until availability, authorisation and simultaneous requests meet. This project demonstrates incremental backend modernisation: secure configuration, relational consistency, explicit architecture boundaries, reproducible integration tests and, in later phases, domain state machines and concurrency control for the last available item.

## Current capabilities

- Versioned REST endpoints for customers, catalogue exemplars and loans.
- JWT-based login and a stateless Spring Security filter chain.
- Persisted user registration with BCrypt hashing and duplicate-email protection.
- Feature-oriented modular monolith under `com.ricardoporto.lending`.
- Thin controllers backed by transactional application services.
- Explicit DTO mappers; JPA entities do not cross the HTTP boundary.
- RFC 9457 Problem Details for validation, not-found, conflict and internal errors.
- PostgreSQL persistence managed by Flyway and validated by Hibernate.
- Integration, migration, security and architecture tests against PostgreSQL 17.
- Reproducible Java 21 build, Maven Wrapper and automated formatting checks.
- Runtime credentials and secrets supplied exclusively through environment variables.

## Modernisation status

| Phase | Status | Outcome |
|---|---|---|
| 0 — Audit and baseline | Complete | Legacy domain, endpoints, risks and test behaviour mapped |
| 1 — Hygiene, security and build | Complete | Java 21 baseline, externalised secrets, reproducible build, Testcontainers and formatting gate |
| 2 — PostgreSQL and Flyway | Complete | PostgreSQL, versioned migrations, constraints, indexes and schema validation |
| 3 — Architecture and error contracts | Complete | Professional package, feature modules, transactional services, DTO boundaries and RFC 9457 errors |
| 4 — Authentication and RBAC | Next | Refresh-token lifecycle, roles, ownership and authorization tests |
| 5–7 — Domain workflows | Planned | Inventory, lending workflow, reservations and concurrency |
| 8–10 — Operations and portfolio | Planned | Observability, Docker Compose, CI and final architecture documentation |

## Architecture

```mermaid
flowchart LR
    Client[API client] --> Security[Spring Security + JWT filter]
    Security --> Controllers[Thin REST controllers]
    Controllers --> Services[Transactional application services]
    Services --> Mappers[Explicit response mappers]
    Services --> Repositories[Internal Spring Data repositories]
    Repositories --> DB[(PostgreSQL)]
    Flyway[Flyway migrations] --> DB
    Controllers -. failures .-> Errors[RFC 9457 exception handler]
    Errors --> Client
    OpenAPI[Swagger UI / OpenAPI] --> Controllers
```

```text
com.ricardoporto.lending
├── auth       login and email-confirmation use cases
├── customer   current borrower model and API
├── resource   current catalogue/exemplar model and API
├── loan       current lending model and API
├── user       identity persistence and registration
└── shared
    ├── config
    ├── exception
    └── security
```

Controllers depend only on application services. Services own transaction boundaries and coordinate repositories. Spring Data REST was removed so repositories cannot accidentally expose entities outside the documented API. A structural test protects the controller boundary.

The decision and trade-offs are recorded in [ADR-001: Feature-oriented modular monolith](docs/adr/001-feature-modular-monolith.md).

## Technology baseline

| Area | Technology |
|---|---|
| Language | Java 21 LTS |
| Framework | Spring Boot 3.4.4, Spring Web, Spring Data JPA |
| Security | Spring Security, BCrypt, Auth0 Java JWT 4.4.0 |
| Database | PostgreSQL 17, Flyway, Hibernate schema validation |
| API documentation | springdoc-openapi 2.8.8 |
| Testing | JUnit 5, Spring Boot Test, MockMvc, Testcontainers 2.0.5 |
| Build and quality | Maven Wrapper 3.9.9, Maven Enforcer, Spotless 3.10.2 |

## Run the tests

Requirements:

- Java 21 or newer;
- Docker Desktop or another Docker-compatible engine.

```bash
./mvnw clean verify
```

On Windows:

```powershell
.\mvnw.cmd clean verify
```

No locally installed database or database credentials are required. The command starts a pinned `postgres:17.6-alpine` container, applies production migrations and test-only fixtures, runs all 20 tests, checks formatting and packages the executable JAR.

## Run the API locally

Create a local configuration file from the safe template:

```bash
cp .env.example .env
```

PowerShell equivalent:

```powershell
Copy-Item .env.example .env
```

Replace every placeholder in `.env`, start PostgreSQL according to `DB_URL`, then run:

```bash
./mvnw spring-boot:run
```

Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

### Environment variables

| Variable | Required | Purpose |
|---|---:|---|
| `DB_URL` | Yes | PostgreSQL JDBC URL |
| `DB_USERNAME` | Yes | Database user supplied by the runtime environment |
| `DB_PASSWORD` | Yes | Database password supplied by the runtime environment |
| `JWT_SECRET` | Yes | HMAC signing secret; use at least 32 random characters |
| `EMAIL_CONFIRMATION_TOKEN` | Yes | Transitional token for the legacy confirmation endpoint |

`.env` files are ignored by Git. Only `.env.example`, containing placeholders, is versioned.

## Database migrations

Flyway is the only source of truth for the runtime schema. The initial production migration is located at:

```text
src/main/resources/db/migration/V1__initial_schema.sql
```

It creates the legacy-compatible domain tables with primary keys, foreign keys, uniqueness rules, checks and indexes. Hibernate runs with `ddl-auto=validate`, so a mismatch fails startup rather than silently modifying the database.

Synthetic accounts live in `src/test/resources/db/testdata/R__test_data.sql`. That location is enabled only by the `test` profile and is never packaged as production seed data.

## API and errors

The generated contract and interactive documentation are available at:

```text
GET /v3/api-docs
GET /swagger-ui/index.html
```

Explicit controllers use the `/api/v1` prefix. Existing Portuguese route names are retained during the bounded migration and will be replaced alongside their domain models in later phases.

Errors use `application/problem+json` and follow RFC 9457. Stable `code` values let clients react without parsing human-readable text; validation failures also include a field-level `errors` map.

```json
{
  "type": "https://resource-lending-api.dev/problems/resource-not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Customer with identifier 999 was not found.",
  "instance": "/api/v1/clientes/999",
  "code": "RESOURCE_NOT_FOUND",
  "timestamp": "2026-09-19T00:00:00Z"
}
```

Unexpected exceptions are logged internally and return a generic response without exposing stack traces or internal exception messages.

## Build quality

`mvn verify` enforces the minimum Java and Maven versions, compiles, tests, packages the application and runs Spotless. Formatting can also be checked or applied independently:

```bash
./mvnw spotless:check
./mvnw spotless:apply
```

The executable artifact is produced at `target/resource-lending-api-0.0.1-SNAPSHOT.jar`.

## Security posture

Completed through Phase 3:

- secrets and database credentials are externalised;
- production seed accounts were removed;
- integration secrets are generated at runtime;
- repositories are internal and cannot be exposed automatically;
- registration normalises e-mail, hashes passwords and rejects duplicates;
- API failures do not leak stack traces or internal exception details.

Known limitations remain visible:

- access-token refresh, logout and revocation are not implemented yet;
- email confirmation uses a transitional environment-supplied token and is not tied to a persisted lifecycle;
- fine-grained RBAC and ownership checks belong to Phase 4;
- the current customer, exemplar and loan models remain transitional until the domain phases.

This baseline supports continued engineering work but is not presented as production-ready.

## Roadmap highlights

1. Implement refresh-token rotation, revocation, RBAC and ownership rules.
2. Introduce the resource catalogue and individually lendable resource items.
3. Model explicit loan and reservation state machines with policy-driven due dates.
4. Protect the last available item with transactional locking and a reproducible concurrency test.
5. Add audit events, correlation IDs and Actuator health checks.
6. Deliver Docker Compose, GitHub Actions, JaCoCo and verified API examples.

## Repository history

The Git history and original authorship are preserved. The academic implementation remains part of the engineering story: each modernisation phase starts from measured behaviour, introduces a bounded change and verifies the result before moving forward.
