# ADR 007: Persisted idempotency for commands

## Status

Accepted.

## Context

Clients commonly retry requests after timeouts without knowing whether the server committed the first attempt. Retrying loan and reservation commands without a protocol-level safeguard can create duplicate records or repeat state transitions.

## Decision

Accept an optional `Idempotency-Key` on duplicate-sensitive `POST` commands. Scope each key to the authenticated user and concrete endpoint, and atomically claim it before the controller executes.

Persist a SHA-256 request fingerprint and the original response status, body, content type and location. An identical completed retry receives that response with `Idempotency-Replayed: true`. The same key with a different fingerprint, or a simultaneous request while the first is processing, receives a stable `409` Problem Details response.

Records expire after a configurable retention period and a scheduled cleanup removes expired rows.

## Consequences

- Network retries cannot duplicate protected commands once a claim exists.
- Keys can be safely reused by different users or on different endpoints.
- A crash after the domain commit but before response capture leaves the key in `PROCESSING` until expiry, choosing duplicate prevention over automatic re-execution.
- Request bodies and responses consume database storage for the retention window; sensitive authentication endpoints are deliberately outside this mechanism.
- Clients that omit the header retain the previous API behavior.
