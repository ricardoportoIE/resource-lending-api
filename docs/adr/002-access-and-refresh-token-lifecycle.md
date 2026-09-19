# ADR-002: Access and refresh-token lifecycle

- Status: Accepted
- Date: 2026-09-19

## Context

The legacy API issued a two-hour JWT and had no refresh, logout or server-side revocation mechanism. It also used a fixed environment token to simulate e-mail confirmation and only distinguished generic user and administrator roles. Phase 4 requires short-lived access, revocable refresh credentials, explicit roles and ownership enforcement without introducing an external identity provider.

## Decision

- Access tokens are signed JWTs with a 15-minute lifetime. They identify the user by normalized e-mail; current roles and account status are reloaded from PostgreSQL on each request.
- Refresh tokens are 384-bit random opaque values with a seven-day lifetime. The plaintext value is returned once, while only its SHA-256 hash is persisted.
- A refresh operation locks the database row, revokes the presented token and creates a replacement in one transaction.
- Reuse of a revoked refresh token revokes every active refresh token for that user. The transaction commits this defensive action even though the request returns HTTP 401.
- Logout accepts a refresh token and revokes it idempotently. Existing access tokens remain valid until their short expiry.
- Authorization uses `STUDENT`, `STAFF` and `ADMIN`. Customer ownership bridges the transitional domain to a user; loan ownership is derived through its customer.
- Registration assigns `STUDENT` and activates the account. The fixed-token e-mail simulation is removed because a real notification integration is outside the MVP.
- The signing secret is supplied only through `JWT_SECRET`; token lifetimes are non-secret configuration.

## Consequences

Compromised refresh tokens can be revoked and replay becomes detectable. Database-backed rotation adds one transaction per refresh and locks competing refresh attempts so only one can succeed. Logout cannot immediately invalidate an already issued JWT without adding an access-token denylist; the 15-minute lifetime bounds that exposure. The nullable ownership column preserves the existing data model while Phase 5 and Phase 6 replace it with the final resource and loan domains.
