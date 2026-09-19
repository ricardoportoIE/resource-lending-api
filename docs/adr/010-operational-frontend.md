# ADR-010: Small role-aware operational frontend

## Status

Accepted.

## Context

OpenAPI proves the HTTP contract but does not demonstrate the complete lending workflow to a non-API audience. A portfolio interface should exercise the real backend without creating a second source of business rules.

## Decision

Build a focused React and TypeScript single-page application. It calls only versioned API endpoints and uses `/api/v1/users/me` as the authority for identity and role-aware navigation. The UI derives available staff actions from the documented loan state machine for presentation, while the backend remains the final authorization and transition authority.

Use an unprivileged Nginx container for static assets and same-origin `/api` reverse proxying. Store tokens in tab-scoped `sessionStorage`; do not persist them in `localStorage`. Generate a fresh `Idempotency-Key` for each duplicate-sensitive command.

## Consequences

- The repository is demonstrable as an end-to-end product through one Compose command.
- Students can self-register, browse inventory, request loans and join queues.
- Staff see the loan desk, operational dashboard and CSV export.
- Same-origin proxying avoids broad CORS configuration in the default deployment.
- This UI intentionally remains an operational console, not a separate domain implementation or design system.
