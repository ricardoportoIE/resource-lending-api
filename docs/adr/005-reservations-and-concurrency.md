# ADR-005: FIFO reservations and pessimistic item locking

- Status: Accepted
- Date: 2026-09-19

## Context

Availability checks alone are unsafe: two transactions can both observe the final item as available and create competing loans. Reservations also require deterministic ordering and must transfer an item when a pickup window expires or a loan is returned.

## Decision

- Reservations form a FIFO queue per catalogue resource and use `WAITING`, `READY`, `FULFILLED`, `EXPIRED` and `CANCELLED` states.
- A ready reservation is assigned one physical item and reserves it for a configurable pickup window.
- Return, cancellation and scheduled expiry promote the oldest waiting reservation in the same transaction.
- Loan requests and item hand-off acquire a PostgreSQL pessimistic write lock on the `resource_items` row.
- A partial unique index permits only one open loan (`REQUESTED`, `APPROVED`, `ACTIVE` or `OVERDUE`) per item as the final database-level defence.
- A concurrent integration test starts two transactions against the same item and verifies exactly one succeeds.

## Consequences

The last available unit cannot be double-booked, including under concurrent requests. Operations for the same item serialize briefly, while unrelated items continue independently. PostgreSQL-specific partial indexes are intentional because PostgreSQL is the supported production and integration database.
