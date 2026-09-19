# ADR-009: OpenTelemetry tracing and domain metrics

## Status

Accepted.

## Context

HTTP logs and generic JVM metrics identify symptoms, but they do not connect a request to database work or scheduled jobs. Operational questions also need domain signals that infrastructure metrics cannot infer reliably.

## Decision

Use Micrometer Observation as the application instrumentation API and the OpenTelemetry bridge with OTLP export. Spring instruments HTTP requests; an application aspect creates bounded, low-cardinality observations for repositories, reporting queries and scheduled jobs.

Publish these domain metrics through the existing Prometheus registry:

- `loans_requested_total` — accepted loan requests;
- `loans_overdue` — current overdue-loan gauge;
- `reservation_queue_wait_seconds` — queue-wait histogram;
- `loan_transition_conflicts_total` — rejected state transitions.

The local Compose profile runs Prometheus, Grafana and Jaeger. Its management port binds only to loopback. Prometheus may read health and metrics without application credentials only when the explicit `INTERNAL_METRICS_ENABLED` switch is enabled by Compose; normal application runs keep operational endpoints protected by `ADMIN`.

## Consequences

- Logs, metrics and traces share the same request trace identifiers.
- Operators can move from a dashboard symptom to a trace and then to the correlated ECS log.
- Metric and span tags must remain low-cardinality; user, loan and resource identifiers are deliberately excluded.
- The local stack is demonstrative rather than a production retention or alerting design.
- Full sampling is convenient in Compose; other environments should tune `TRACING_SAMPLING_PROBABILITY` for their traffic and cost profile.
