# ADR 006: Transactional Outbox for external notifications

## Status

Accepted.

## Context

Loan approvals, due-soon reminders and ready reservations must eventually notify an external system. Publishing directly inside a database transaction can either lose an event after commit or notify a recipient for a transaction that later rolls back.

## Decision

Store notification events in `outbox_events` within the same transaction as the domain change. A scheduled worker locks a bounded due batch, sends each event through a delivery adapter and records the successful `(event, channel)` pair in `notification_deliveries`.

Retries use exponential backoff and stop after a configured maximum. Exhausted events remain queryable through an `ADMIN` endpoint. Repeated domain scans use a unique semantic deduplication key, while webhook calls carry the event UUID as `Idempotency-Key`.

The default adapter writes a structured delivery marker to the application log. Setting `NOTIFICATION_WEBHOOK_URL` enables real HTTP delivery without changing domain code.

## Consequences

- Domain state and intent to notify commit atomically.
- Delivery is at least once across process failures; downstream webhook consumers must honor the idempotency key.
- Duplicate processing in this service is prevented after a recorded success.
- Holding row locks during network delivery favors correctness and simplicity, but batch size and timeout must remain bounded.
- Failed events are retained for diagnosis instead of being silently discarded.
