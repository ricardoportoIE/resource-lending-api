# Resource Lending Console

React and TypeScript operational UI for the Resource Lending API. It supports student self-registration, e-mail confirmation, login, password recovery, catalogue browsing, idempotent loan requests, reservations, role-aware staff transitions, operational metrics and CSV export.

## Local development

Run the API on port `8080`, then:

```bash
npm install
npm run dev
```

Vite proxies `/api` to the backend. Production uses the included multi-stage Dockerfile and an unprivileged Nginx process on port `8080`.

Verification commands:

```bash
npm run check
npm test
npm run build
npm audit --audit-level=moderate
```

Access and refresh tokens are held in `sessionStorage`, so closing the tab clears the browser session. Signing out also calls the server-side access-token revocation endpoint. Staff and administrator accounts are provisioned outside public registration to prevent role escalation.

The API client retries an authenticated request once after rotating an expired access token. Network, reverse-proxy and RFC 9457 errors are converted into actionable UI messages, and concurrent command clicks are suppressed while an operation is running. The production Nginx proxy preserves the browser-facing host and port so same-origin authentication requests are not incorrectly rejected by the backend CORS boundary.
