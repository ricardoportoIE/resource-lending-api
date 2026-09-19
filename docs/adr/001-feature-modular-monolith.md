# ADR-001: Feature-oriented modular monolith

- Status: Accepted
- Date: 2026-09-19

## Context

The inherited application used an academic base package and split closely related domain types across many small packages. REST controllers coordinated persistence, subtype construction, mapping and error handling directly. Spring Data REST was also present, allowing repository endpoints to bypass the explicit API boundary.

The project needs clear ownership boundaries without prematurely introducing distributed services.

## Decision

Use a modular monolith rooted at `com.ricardoporto.lending` and group code by business feature:

- `auth` for login and refresh-token use cases;
- `resource` for catalogue resources and physical inventory;
- `loan` for the policy-driven lending workflow;
- `reservation` for queueing, pickup windows and promotion;
- `audit` for immutable domain transition records;
- `user` for identity persistence and registration;
- `shared` for cross-cutting configuration, security and error handling.

Controllers are HTTP adapters and depend only on application services. Services own transaction boundaries and coordinate repositories. Mappers build response DTOs so entities do not cross the API boundary. Repositories are internal persistence details; Spring Data REST is removed.

API failures use RFC 9457 `ProblemDetail` responses with a stable application code and timestamp. Unexpected exceptions are logged server-side and return a generic detail without leaking internal messages.

## Consequences

- Feature ownership and transaction boundaries are visible in the source tree.
- Controllers can no longer call repositories directly; a structural test protects this rule.
- Versioned `/api/v1` routes remain stable while internals can evolve independently.
- The application remains one deployable unit and one database, avoiding distributed-system overhead.
- Superseded academic controllers and entities were removed after their replacement modules became complete; their tables remain preserved in PostgreSQL schema `legacy`.
