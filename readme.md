# Resource Lending API

[![CI](https://github.com/ricardoportoIE/resource-lending-api/actions/workflows/ci.yml/badge.svg)](https://github.com/ricardoportoIE/resource-lending-api/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 3.4.4](https://img.shields.io/badge/Spring_Boot-3.4.4-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Tests](https://img.shields.io/badge/backend_tests-29_passing-brightgreen)
![Coverage](https://img.shields.io/badge/line_coverage-85.74%25-brightgreen)

A production-oriented REST API for lending shared organisational resources: books, laptops, rooms, tools and other individually tracked assets. It handles catalogue inventory, policy-driven loans, FIFO reservations and simultaneous claims without lending the same physical item twice.

This repository is also a modernization case study. An academic library CRUD application was incrementally rebuilt into a secure, observable and containerised Java 21 service while preserving its Git history and archiving the superseded schema without destroying data.

## Engineering highlights

- Explicit loan state machine instead of a generic status update.
- Pessimistic row locking plus partial unique indexes for last-item concurrency safety.
- FIFO reservation queue with ready windows, expiry and automatic promotion on return.
- Short-lived JWT access tokens and opaque, hashed, rotating refresh tokens with reuse detection.
- `STUDENT`, `STAFF` and `ADMIN` authorization at both HTTP and service boundaries.
- Policy-driven due dates and active-loan limits by role and resource type.
- Transactional, immutable audit events for domain transitions.
- Transactional Outbox notifications with idempotent delivery, retries and exponential backoff.
- Persistent `Idempotency-Key` replay for duplicate-sensitive loan and reservation commands.
- Staff operational dashboard, demand/utilization analytics and RFC 4180-compatible CSV exports.
- OpenTelemetry traces across HTTP, repositories and scheduled jobs, with four domain metrics.
- Responsive React/TypeScript console for borrowers and role-aware staff operations.
- RFC 9457 Problem Details with stable error codes and correlation IDs.
- Flyway-only PostgreSQL schema management with Hibernate validation.
- Testcontainers integration and concurrency tests against PostgreSQL 17.
- ECS-compatible JSON logs plus a provisioned Prometheus, Grafana and Jaeger stack.
- Multi-stage non-root image, health-checked Docker Compose and GitHub Actions CI.
- JaCoCo coverage gates and Spotless formatting enforcement.

## Architecture at a glance

```mermaid
flowchart LR
    Browser[React operations console] --> Proxy[Nginx same-origin proxy]
    Client[API client] --> Security[JWT authentication and RBAC]
    Proxy --> Security
    Security --> Controllers[REST controllers]
    Controllers --> Services[Transactional application services]
    Services --> Locks[Pessimistic item locks]
    Services --> Repositories[Spring Data repositories]
    Repositories --> DB[(PostgreSQL 17)]
    Flyway[Flyway migrations] --> DB
    Services --> Audit[Audit events]
    Services --> Outbox[(Transactional Outbox)]
    Outbox --> Worker[Retrying notification worker]
    Worker --> External[Log or webhook adapter]
    Scheduler[Reservation expiry scheduler] --> Services
    Observability[Micrometer and OpenTelemetry] -. observes .-> Services
    Observability --> Prometheus[Prometheus]
    Observability --> Jaeger[Jaeger]
    Prometheus --> Grafana[Grafana dashboard]
    Jaeger --> Grafana
```

The code is a feature-oriented modular monolith under `com.ricardoporto.lending`. Controllers depend on application services, services own transaction boundaries, repositories remain internal and JPA entities never cross the HTTP boundary.

Detailed, code-aligned diagrams are in [Architecture and domain flows](docs/architecture.md). Design trade-offs are recorded in [Architecture Decision Records](docs/adr/).

## Loan lifecycle

```mermaid
stateDiagram-v2
    [*] --> REQUESTED
    REQUESTED --> APPROVED: staff approves
    REQUESTED --> REJECTED: staff rejects
    REQUESTED --> CANCELLED: borrower or staff cancels
    APPROVED --> ACTIVE: item collected
    APPROVED --> CANCELLED: borrower or staff cancels
    ACTIVE --> OVERDUE: due date passes
    ACTIVE --> RETURNED: item returned
    OVERDUE --> RETURNED: item returned
```

Every transition has a dedicated command endpoint. Invalid transitions return HTTP `409` with a stable code instead of silently changing state.

## Run with Docker

Requirements: Docker Desktop or another Docker-compatible engine.

```bash
cp .env.example .env
```

PowerShell:

```powershell
Copy-Item .env.example .env
```

Replace the four required placeholders in `.env` (`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` and `GRAFANA_ADMIN_PASSWORD`), then start the complete stack:

```bash
docker compose up --build
```

The database is started first and must pass its health check before the API starts. Flyway builds the schema automatically. The Java process runs as the unprivileged `app` user.

| URL | Purpose | Access |
|---|---|---|
| `http://localhost:8080/swagger-ui/index.html` | Interactive API documentation | Public |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON | Public |
| `http://localhost:8081/actuator/health` | Compose management health | Loopback only |
| `http://localhost:9090` | Prometheus query UI | Loopback only |
| `http://localhost:3000` | Provisioned Grafana dashboard | `.env` credentials |
| `http://localhost:16686` | Jaeger trace explorer | Loopback only |
| `http://localhost:3001` | React operational console | Loopback only |

Stop the stack while retaining database data:

```bash
docker compose down
```

Add `--volumes` only when you intentionally want to delete the local database volume.

## Run the verification suite

Requirements: Java 21+ and Docker.

```bash
./mvnw clean verify
```

PowerShell:

```powershell
.\mvnw.cmd clean verify
```

The build starts an isolated `postgres:17.6-alpine` Testcontainer, applies every production migration from an empty database, runs the complete test suite, packages the executable JAR, checks formatting and enforces at least 75% line and 35% branch coverage. The HTML report is generated at `target/site/jacoco/index.html`.

GitHub Actions repeats backend and frontend verification on every push and pull request to `main`, audits npm dependencies, uploads the coverage report and builds both production images.

## API surface

All business endpoints are versioned under `/api/v1`.

| Area | Endpoints |
|---|---|
| Authentication | `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`; `GET /users/me` |
| Catalogue | `GET/POST /resources`, `GET/PATCH /resources/{id}` |
| Inventory | `POST /resources/{id}/items`, `PATCH /resource-items/{id}/status` |
| Loans | `POST/GET /loans`, `GET /loans/{id}` |
| Loan commands | `POST /loans/{id}/approve`, `/reject`, `/collect`, `/return`, `/cancel` |
| Reservations | `POST /resources/{id}/reservations`, `GET /reservations`, `DELETE /reservations/{id}` |
| Outbox operations | `GET /admin/outbox-events?status=FAILED` (`ADMIN` only) |
| Operational reports | `GET /reports/dashboard`, `/loans`, `/resource-utilization`, `/reservation-wait`, `/popular-resources`, `/unavailable-items`, `/export.csv` |

Paginated catalogue queries support `type`, `category`, `status`, `page`, `size` and `sort` parameters. The generated OpenAPI document is the source of truth for request and response schemas.

Complete, copyable authentication and workflow requests are in [cURL examples](docs/api-examples.md).

Loan creation, approval, collection, return and reservation creation accept an optional `Idempotency-Key` header. The key is scoped to the authenticated user and endpoint. An identical retry replays the original HTTP status, body, content type and location without executing the command again; a changed payload with the same key returns `409 IDEMPOTENCY_KEY_REUSED`. Records expire after the configured retention window.

## Authorization model

| Capability | STUDENT | STAFF | ADMIN |
|---|:---:|:---:|:---:|
| Browse resources and inventory | Yes | Yes | Yes |
| Request and read own loans | Yes | Yes | Yes |
| Read all loans | No | Yes | Yes |
| Approve, reject, collect or return loans | No | Yes | Yes |
| Cancel own requested/approved loan | Yes | Yes | Yes |
| Manage catalogue and item status | No | Yes | Yes |
| Create, list and cancel own reservations | Yes | Yes | Yes |
| List or cancel any reservation | No | Yes | Yes |
| Read protected operational endpoints | No | No | Yes |
| Read dashboards and export reports | No | Yes | Yes |

Unauthenticated requests return `401`; authenticated callers without permission return `403`. Students cannot select another borrower when requesting a loan.

## Error contract

Failures use `application/problem+json` and RFC 9457. Clients can depend on `code` without parsing human-readable messages.

```json
{
  "type": "https://resource-lending-api.dev/problems/resource-not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Resource with identifier 97f1... was not found.",
  "instance": "/api/v1/resources/97f1...",
  "code": "RESOURCE_NOT_FOUND",
  "timestamp": "2026-09-19T00:00:00Z",
  "correlationId": "d8b1181c-..."
}
```

Every response includes `X-Correlation-ID`. A safe client-supplied ID is preserved; otherwise the API generates a UUID and includes it in structured logs and error responses. Passwords, access tokens and refresh tokens are redacted from application object logging.

## Technology

| Area | Choice |
|---|---|
| Language and framework | Java 21, Spring Boot 3.4.4 |
| HTTP and persistence | Spring Web, Spring Data JPA, Hibernate validation |
| Security | Spring Security, BCrypt, Auth0 Java JWT 4.4.0 |
| Database | PostgreSQL 17, Flyway |
| Documentation | springdoc-openapi 2.8.8, Swagger UI |
| Frontend | React 19, TypeScript 7, Vite 8, unprivileged Nginx |
| Observability | Micrometer, OpenTelemetry/OTLP, Prometheus 3, Grafana 13, Jaeger 2, ECS logging |
| Testing | JUnit 5, MockMvc, Testcontainers 2.0.5 |
| Quality and delivery | Maven Wrapper 3.9.9, Enforcer, Spotless, JaCoCo, Docker, GitHub Actions |

## Configuration

Runtime secrets are environment-only. `.env` is ignored by Git; the versioned `.env.example` contains placeholders.

| Variable | Required | Default / purpose |
|---|:---:|---|
| `DB_URL` | Yes outside Compose | PostgreSQL JDBC URL; Compose injects its internal URL |
| `DB_USERNAME` | Yes | PostgreSQL user |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | HMAC secret with at least 32 random characters |
| `CORS_ALLOWED_ORIGINS` | No | Empty means browser cross-origin access is denied |
| `RESERVATION_READY_WINDOW` | No | `P2D` |
| `RESERVATION_EXPIRY_SCAN_MS` | No | `60000` |
| `OUTBOX_POLL_MS` | No | Delivery worker interval; `5000` |
| `OUTBOX_BATCH_SIZE` | No | Events locked per worker run; `25` |
| `OUTBOX_MAX_ATTEMPTS` | No | Attempts before an event becomes `FAILED`; `5` |
| `OUTBOX_RETRY_BASE` | No | ISO-8601 exponential backoff base; `PT30S` |
| `NOTIFICATION_WEBHOOK_URL` | No | Blank uses the fake log adapter; otherwise receives JSON via POST |
| `NOTIFICATION_DUE_SOON_WINDOW` | No | Lead time for due-soon events; `PT24H` |
| `IDEMPOTENCY_RETENTION` | No | Retain replayable command responses; `P1D` |
| `IDEMPOTENCY_CLEANUP_MS` | No | Expired-record cleanup interval; `3600000` |
| `LOG_FORMAT` | No | `ecs`; use `plain` for local human-readable logs |
| `APP_PORT` | No | Host port `8080` in Docker Compose |
| `MANAGEMENT_PORT` | No | Loopback-only Compose management port; `8081` |
| `PROMETHEUS_PORT` | No | Loopback-only Prometheus port; `9090` |
| `GRAFANA_PORT` | No | Loopback-only Grafana port; `3000` |
| `JAEGER_UI_PORT` | No | Loopback-only Jaeger UI port; `16686` |
| `FRONTEND_PORT` | No | Loopback-only operational console port; `3001` |
| `GRAFANA_ADMIN_PASSWORD` | Yes in Compose | Local Grafana administrator password |
| `TRACING_SAMPLING_PROBABILITY` | No | Trace sample ratio; Compose uses `1.0`, application default is `0.0` |

## Database evolution and modernization

Flyway is the only schema authority and Hibernate runs with `ddl-auto=validate`. Eight versioned migrations introduce authentication, inventory, loan policies, audit events, reservations, concurrency constraints, the transactional Outbox and command idempotency.

The original `Cliente`, `Exemplar` and `Emprestimo` API was retired after the replacement domain became complete. Migration V6 moves its tables into a dedicated `legacy` schema rather than dropping them, preserving historical data while keeping the active `public` schema and OpenAPI contract focused on resources, items, loans and reservations.

| Modernization stage | Result |
|---|---|
| Baseline and hygiene | Reproducible Java 21 build, externalised secrets and PostgreSQL integration tests |
| Persistence and architecture | Flyway migrations, constraints, feature modules, DTO boundaries and Problem Details |
| Identity and authorization | JWT/refresh lifecycle, role-based access and ownership checks |
| Domain redesign | Typed inventory, policy-based state machine and audit trail |
| Concurrency | FIFO reservations, expiry, row locks and database uniqueness backstops |
| Operations and delivery | OpenAPI, structured logs, metrics, Docker Compose, coverage gates and CI |
| Portfolio finish | Legacy API retirement, faithful diagrams and verified examples |
| Async integration | Transactional Outbox, due-soon jobs, idempotent notification delivery and failed-event operations |
| Reliable commands | Atomic idempotency claims, request fingerprinting and exact response replay |
| Operational intelligence | Staff dashboards, current utilization, demand ranking, queue wait time and CSV export |
| Distributed observability | OTLP traces, domain metrics and a provisioned Prometheus/Grafana/Jaeger stack |
| Demonstrable product | Role-aware React console for catalogue, lending, reservations and operations |

The project now lives in the professional GitHub account [`ricardoportoIE`](https://github.com/ricardoportoIE/resource-lending-api); the repository history retains the original authorship and the complete modernization journey.

## Further reading

- [Architecture, domain model and concurrency flows](docs/architecture.md)
- [Verified cURL workflow](docs/api-examples.md)
- [Observability and troubleshooting runbook](docs/troubleshooting.md)
- [Frontend development and verification](frontend/README.md)
- [Recruiter, CV and GitHub summary](docs/portfolio-summary.md)
- [ADR-001: Feature-oriented modular monolith](docs/adr/001-feature-modular-monolith.md)
- [ADR-002: Access and refresh-token lifecycle](docs/adr/002-access-and-refresh-token-lifecycle.md)
- [ADR-003: Resource and ResourceItem](docs/adr/003-resource-and-resource-item.md)
- [ADR-004: Explicit loan workflow](docs/adr/004-explicit-loan-workflow.md)
- [ADR-005: Reservations and concurrency](docs/adr/005-reservations-and-concurrency.md)
- [ADR-006: Transactional Outbox](docs/adr/006-transactional-outbox.md)
- [ADR-007: Command idempotency](docs/adr/007-command-idempotency.md)
- [ADR-008: Operational reporting read model](docs/adr/008-operational-reporting-read-model.md)
- [ADR-009: Distributed observability](docs/adr/009-distributed-observability.md)
- [ADR-010: Operational frontend](docs/adr/010-operational-frontend.md)

## Scope boundaries

The current implementation uses a fake log/webhook notification adapter and a single-process scheduler. Cloud infrastructure and rate limiting remain extension points rather than features presented as complete. Access tokens remain valid until their short expiry after logout; refresh tokens are revoked server-side immediately.
