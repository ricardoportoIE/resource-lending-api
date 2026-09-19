# ADR-011: Layered identity and abuse controls

## Status

Accepted.

## Decision

Apply complementary controls at the HTTP, identity-token and persistence boundaries:

- fixed-window request limits keyed by authenticated user or direct remote IP;
- exponential temporary account lock after repeated login failures;
- hashed, one-time email confirmation and password-reset tokens delivered through the transactional Outbox;
- JWT `kid`, `jti` and account security-version claims, with a configurable verification key ring and persistent access-token revocation;
- restrictive response headers and narrow CORS defaults;
- CodeQL, dependency review, Dependabot and scheduled OWASP ZAP automation.

Password reset increments the account security version and revokes all refresh tokens, invalidating existing sessions. Public recovery responses do not disclose whether an e-mail address exists.

## Consequences

- A single process can enforce abuse limits without external infrastructure.
- Multi-replica deployments must move rate-limit state to an edge gateway or shared store.
- Previous JWT keys must remain configured for at least the access-token lifetime during rotation.
- Identity delivery payloads contain short-lived raw tokens because the external recipient needs them; database access and Outbox retention therefore remain security boundaries.
