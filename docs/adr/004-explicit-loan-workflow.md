# ADR-004: Explicit loan state machine and policy lookup

- Status: Accepted
- Date: 2026-09-19

## Context

The legacy loan API exposes a generic patch operation over dates and relationships. That permits invalid business transitions and cannot consistently enforce availability, borrower limits, overdue blocking or due-date calculation.

## Decision

The new `Loan` aggregate uses the states `REQUESTED`, `APPROVED`, `ACTIVE`, `OVERDUE`, `RETURNED`, `CANCELLED` and `REJECTED`. Each supported transition has a dedicated service operation and HTTP endpoint. Invalid transitions return HTTP 409.

Policies are persisted per borrower role and resource type. Requesting checks account status, item availability, open requests, overdue loans and the active-loan limit. Collection calculates `dueAt` from the applicable policy and changes the physical item to `ON_LOAN`; return releases it to `AVAILABLE`. Every transition records an audit event in the same transaction.

## Consequences

Clients cannot bypass workflow rules with arbitrary patches, and policy changes remain data-driven. The legacy `Emprestimo` API is retained temporarily for migration compatibility but is no longer the target domain. Locking and database-level race protection are deliberately completed in Phase 7.
