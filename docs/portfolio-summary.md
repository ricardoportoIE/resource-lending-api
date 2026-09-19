# Portfolio summary

## GitHub description

Production-oriented Java 21/Spring Boot API for resource lending, FIFO reservations and concurrency-safe inventory, with PostgreSQL, JWT/RBAC, Testcontainers, Docker and CI.

## CV version

**Resource Lending Management API — Java / Spring Boot**  
Re-engineered a university library system into a production-oriented REST API for organisational resource lending, reservations and returns. Built with Java 21, Spring Boot and PostgreSQL, with JWT authentication, rotating refresh tokens, role-based access control, policy-driven transactional workflows, concurrency protection, Flyway migrations, Testcontainers integration tests, OpenAPI, observability, Docker and GitHub Actions CI.

## Interview talking points

- Separated catalogue descriptions from physical inventory so availability is tracked per asset.
- Replaced unrestricted CRUD updates with explicit state transitions and policy checks.
- Protected the final available item with pessimistic locking and a database uniqueness constraint, then proved it with a two-thread integration test.
- Modelled FIFO reservations, pickup expiry and automatic promotion as one transactional workflow.
- Stored only refresh-token hashes and implemented rotation, revocation and reuse detection.
- Preserved the academic history while moving superseded tables to a non-destructive `legacy` schema.
- Made delivery reproducible through a non-root image, health-checked Compose stack, coverage gates and CI.

## Repository migration status

The professional-account migration is complete. The canonical remote is:

```text
https://github.com/ricardoportoIE/resource-lending-api.git
```

The original history and authorship remain intact. Any older fork can be archived with a short pointer to the canonical repository; no force-push or history rewrite is required.
