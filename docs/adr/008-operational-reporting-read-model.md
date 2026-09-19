# ADR 008: SQL read model for operational reporting

## Status

Accepted.

## Context

Operational dashboards aggregate loans, reservations, resources and physical items. Loading entity graphs and aggregating in Java would add memory use, query volume and accidental coupling to write-side JPA mappings.

## Decision

Implement a dedicated read-only reporting module backed by explicit PostgreSQL aggregation queries. Return immutable report records and keep all endpoints behind `STAFF`/`ADMIN` authorization.

Define current resource utilization as `ON_LOAN items / total physical items`. Rank demand using the combined number of loan and reservation requests. Calculate queue wait from `created_at` to `ready_at`, and expose the sample size beside the average. Generate CSV from the same read models used by JSON endpoints.

## Consequences

- Reporting queries are visible, testable and optimized for the database.
- The semantics of every metric are stable and documented.
- JSON and CSV cannot silently diverge because they share one query layer.
- This is a synchronous operational read model; large historical datasets may later require materialized views or a warehouse.
