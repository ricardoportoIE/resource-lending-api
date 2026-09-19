# Observability and troubleshooting

The Compose stack provisions a Resource Lending dashboard in Grafana and connects it to Prometheus and Jaeger. Start the stack, generate a few API requests, then open:

- Grafana: `http://localhost:3000` (credentials from `.env`);
- Prometheus: `http://localhost:9090`;
- Jaeger: `http://localhost:16686`;
- management health and raw metrics: `http://localhost:8081/actuator/health` and `/actuator/prometheus`.

The management, Prometheus, Grafana and Jaeger ports bind to `127.0.0.1` in Compose. Do not expose them publicly without authentication and network controls.

## A request or report is slow

1. In Grafana, inspect HTTP latency and the reservation wait panel around the incident time.
2. In Jaeger, select `resource-lending-api`, find the slow `http.server.requests` trace and inspect its `db.repository` or `db.report.query` children.
3. Copy the trace ID into the application logs. ECS records include `traceId`, `spanId` and, for HTTP work, `correlationId`.
4. Compare repeated repository spans before changing database indexes; a high request duration is not by itself proof of a slow query.

Useful PromQL:

```promql
histogram_quantile(0.95,
  sum by (le) (rate(http_server_requests_seconds_bucket[5m])))
```

## Loan transition conflicts rise

Use the conflict rate to distinguish isolated stale clients from a sustained workflow problem:

```promql
sum(rate(loan_transition_conflicts_total[5m]))
```

Find traces for the affected command endpoint, then correlate the trace with its RFC 9457 response and audit event. Conflicts are expected for an invalid source state; a sharp increase can indicate repeated client retries, stale UI state or simultaneous staff actions.

## Reservations wait too long

Compare queue latency with inventory and demand reports:

```promql
histogram_quantile(0.95,
  sum by (le) (rate(reservation_queue_wait_seconds_bucket[30m])))
```

The histogram records the interval from reservation creation until it becomes `READY`. Use `/api/v1/reports/popular-resources` and `/resource-utilization` to decide whether the cause is demand, insufficient inventory or items left unavailable.

## Overdue inventory accumulates

```promql
loans_overdue
```

The value is a live database-backed gauge, not a counter. Cross-check `/api/v1/reports/loans?status=OVERDUE`, then inspect scheduled-job traces if due-soon notifications or state scans also appear delayed.

## Traces or metrics are missing

- Confirm the API health check passes on management port `8081`.
- In Prometheus, check **Status > Target health** and verify `api:8081` is `UP`.
- Verify `TRACING_SAMPLING_PROBABILITY` is greater than zero and the API uses `http://jaeger:4318/v1/traces` inside Compose.
- Search API logs for OTLP export errors. Jaeger startup is not health-gated so the exporter can briefly retry while the collector becomes ready.
- Remember that counters and histograms appear after registration; the application registers the four domain instruments during startup.
