# Architecture and domain flows

This document describes the implemented system. It deliberately excludes roadmap ideas and uses the same names and states exposed by the code and OpenAPI contract.

## Component boundaries

```mermaid
flowchart TB
    subgraph HTTP[HTTP boundary]
        Correlation[CorrelationIdFilter]
        JWT[SecurityFilter]
        Controllers[Auth, Resource, Loan, Reservation and Report controllers]
        Problems[Global Problem Details handler]
    end

    subgraph Application[Application layer]
        Auth[Login and refresh-token services]
        Catalogue[ResourceCatalogueService]
        Loans[LoanWorkflowService]
        Reservations[ReservationService]
        Audit[AuditService]
        Outbox[Outbox publisher and worker]
    end

    subgraph Persistence[Persistence boundary]
        Repositories[Internal JPA repositories]
        PostgreSQL[(PostgreSQL)]
        Flyway[Flyway V1-V8]
    end

    Correlation --> JWT --> Controllers
    Controllers --> Auth
    Controllers --> Catalogue
    Controllers --> Loans
    Controllers --> Reservations
    Controllers -. exceptions .-> Problems
    Loans --> Audit
    Reservations --> Audit
    Loans --> Outbox
    Reservations --> Outbox
    Auth --> Repositories
    Catalogue --> Repositories
    Loans --> Repositories
    Reservations --> Repositories
    Audit --> Repositories
    Outbox --> Repositories
    Repositories --> PostgreSQL
    Flyway --> PostgreSQL
```

The modules form one deployable process and one transactional database. This keeps consistency rules simple while feature packages and structural tests prevent the codebase from collapsing into controller-to-repository CRUD.

## Logical domain model

```mermaid
erDiagram
    USER ||--o{ REFRESH_TOKEN : owns
    USER ||--o{ LOAN : borrows
    USER ||--o{ LOAN : approves
    USER ||--o{ RESERVATION : queues
    USER ||--o{ AUDIT_EVENT : acts
    USER }o--o{ ROLE : has
    RESOURCE ||--o{ RESOURCE_ITEM : contains
    RESOURCE ||--o{ RESERVATION : requested_for
    RESOURCE_ITEM ||--o{ LOAN : loaned_as
    RESOURCE_ITEM o|--o| RESERVATION : held_for

    RESOURCE {
        uuid id PK
        string name
        enum type
        string category
        string identifier UK
        boolean loanable
    }
    RESOURCE_ITEM {
        uuid id PK
        uuid resource_id FK
        string asset_tag UK
        enum status
        long version
    }
    LOAN {
        uuid id PK
        long borrower_id FK
        uuid resource_item_id FK
        enum status
        timestamp due_at
        long version
    }
    RESERVATION {
        uuid id PK
        long user_id FK
        uuid resource_id FK
        uuid ready_item_id FK
        enum status
        timestamp expires_at
    }
```

`Resource` is the catalogue description; `ResourceItem` is the physical unit that can be reserved or loaned. That distinction allows two laptops of the same model to carry different asset tags and lifecycle states.

Loan policies are reference data keyed by role and resource type. Audit events reference domain entities by type and UUID so the audit store does not participate in their lifecycle.

## Loan state machine

```mermaid
stateDiagram-v2
    [*] --> REQUESTED
    REQUESTED --> APPROVED: approve
    REQUESTED --> REJECTED: reject
    REQUESTED --> CANCELLED: cancel
    APPROVED --> ACTIVE: collect
    APPROVED --> CANCELLED: cancel
    ACTIVE --> OVERDUE: due date elapsed
    ACTIVE --> RETURNED: return
    OVERDUE --> RETURNED: return
```

The service validates the expected source state inside the same transaction that writes the target state and audit event. Due dates are assigned on collection from the matching role/resource policy. A borrower with an overdue loan or a reached active-loan limit cannot open another loan.

## Last-item concurrency

```mermaid
sequenceDiagram
    participant A as Request A
    participant B as Request B
    participant DB as PostgreSQL

    par simultaneous requests
        A->>DB: SELECT ResourceItem FOR UPDATE
        B->>DB: SELECT ResourceItem FOR UPDATE (waits)
    end
    A->>DB: validate AVAILABLE and insert open loan
    A->>DB: COMMIT
    DB-->>B: lock acquired
    B->>DB: validate open-loan constraint
    DB-->>B: conflict: RESOURCE_UNAVAILABLE
```

The application uses a pessimistic write lock while selecting the item. PostgreSQL partial unique index `uk_loans_item_open` is a second line of defence: at most one `REQUESTED`, `APPROVED`, `ACTIVE` or `OVERDUE` loan can reference an item. The integration suite starts two threads against the same item and asserts exactly one succeeds.

## Reservation promotion

```mermaid
flowchart LR
    Request[Reservation created] --> Available{Available item?}
    Available -- Yes --> Ready[READY and item RESERVED]
    Available -- No --> Waiting[WAITING in FIFO queue]
    Return[Loan returned] --> Promote[Promote oldest WAITING]
    Cancel[READY cancelled] --> Promote
    Expire[READY window expires] --> Promote
    Promote --> Ready
    Ready --> Claim[Borrower requests the held item]
    Claim --> Fulfilled[FULFILLED]
```

Open reservations are unique per user/resource. A scheduled scanner expires missed pickup windows; cancellation, expiry and return all reuse the same promotion logic.

## Security and token lifecycle

Access tokens are signed JWTs with a 15-minute lifetime. Refresh tokens are 48-byte opaque values; only their SHA-256 hashes are persisted. Rotation locks the database row, revokes the presented token and issues a replacement. Reusing a revoked token revokes every active refresh token for that user.

URL rules provide early rejection, while method security and ownership checks protect service calls even when they are not reached through the expected controller.

## Schema evolution

Flyway owns the schema and Hibernate only validates it. Migrations are append-only:

1. original schema and identity tables;
2. roles, ownership and refresh tokens;
3. resources and physical items;
4. loans, policies and audit events;
5. reservations and concurrency constraints;
6. retirement of the superseded library model;
7. transactional Outbox and idempotent notification delivery;
8. persistent command idempotency and exact response replay.

V6 moves the original academic tables to PostgreSQL schema `legacy`. This preserves data and migration traceability while keeping the active `public` schema aligned with the current API.

## Operational model

- Structured ECS logs carry `correlationId`, `traceId` and `spanId` without tokens or passwords.
- HTTP requests, repositories, report queries and scheduled jobs emit OpenTelemetry spans over OTLP.
- `/actuator/health` and probe groups are public for orchestrators.
- metrics, Prometheus and info endpoints require `ADMIN` during normal runs. Compose enables a separate loopback-only management chain so its internal Prometheus can scrape health and metrics.
- Domain counters, gauges and histograms describe loan requests, overdue loans, reservation wait and workflow conflicts.
- the final image contains only the runtime and application artifact and runs as user `app`.
- Compose health checks gate API startup on PostgreSQL readiness and provisions Prometheus, Grafana and Jaeger.
