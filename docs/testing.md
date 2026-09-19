# Testing strategy

The repository uses layered automated verification so domain invariants, browser behaviour and security boundaries fail close to the code that owns them. The default suites do not depend on the developer database: backend integration tests start PostgreSQL 17 through Testcontainers and apply all Flyway migrations from an empty schema.

## Verification matrix

| Layer | Tools | Principal guarantees |
|---|---|---|
| Java unit | JUnit 5, Mockito | JWT validation and key rotation, rate-limit isolation, mappings and local policies |
| API integration | Spring Boot, MockMvc, Testcontainers | authentication, RBAC/ownership, Problem Details, CORS, secure headers, workflow transitions and persistence |
| Concurrency and data | JUnit 5, PostgreSQL 17 | last-item exclusion, FIFO reservations, idempotent replay and transactional Outbox delivery |
| Architecture | structural JUnit rules | controllers do not bypass services and feature boundaries remain explicit |
| React unit/component | Vitest, Testing Library, user-event, jsdom | authentication/recovery journeys, role-aware UI, commands, empty/error states and accessibility |
| Browser client security | Vitest | bearer-token scoping, refresh single-flight, logout revocation, session cleanup and idempotency keys |
| Static/dependency analysis | CodeQL, dependency review, npm audit | Java/TypeScript data-flow findings and vulnerable dependency changes |
| Dynamic application security | OWASP ZAP | scheduled OpenAPI-driven scan against the containerized API |

## Run locally

Backend, formatting and coverage gate:

```powershell
.\mvnw.cmd clean verify
```

Frontend type checking, tests, coverage, production compilation and dependency audit:

```powershell
Push-Location frontend
npm ci
npm run check
npm run test:coverage
npm run build
npm audit --audit-level=moderate
Pop-Location
```

The backend HTML coverage report is `target/site/jacoco/index.html`; the frontend report is `frontend/coverage/index.html`. Maven enforces 75% line and 35% branch coverage across the backend. Vitest enforces 80% line, statement and function coverage plus 70% branch coverage for executable frontend modules.

## Security regression catalogue

Backend regression tests cover malformed and incomplete JWTs, issuer/expiry/revocation checks, minimum signing-secret strength, refresh-token rotation and reuse detection, access-token revocation, password-reset session invalidation, enumeration-safe failures, account lockout, independent per-IP/per-user rate windows, CORS denial, role injection, authorization boundaries, correlation-ID sanitization and response security headers.

Frontend regression tests cover credential header scoping, same-origin fetch policy, safe RFC 9457/proxy/network errors, storage denial, refresh failure cleanup, refresh concurrency, no auth-endpoint retries, distinct command idempotency keys, dual logout revocation and privileged-navigation hiding. Component tests verify the user-visible login, registration, confirmation, recovery, catalogue, loan, reservation, staff and dashboard paths.

CI runs the same coverage-gated commands on every push and pull request and uploads both HTML reports. CodeQL analyses Java and TypeScript separately; dependency review is required for pull requests; ZAP runs on its own scheduled or manually triggered workflow.
