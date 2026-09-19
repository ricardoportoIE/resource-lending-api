CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    deduplication_key VARCHAR(200) NOT NULL UNIQUE,
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT chk_outbox_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_outbox_delivery_queue ON outbox_events (status, available_at, created_at);
CREATE INDEX idx_outbox_created_at ON outbox_events (created_at DESC);

CREATE TABLE notification_deliveries (
    id UUID PRIMARY KEY,
    outbox_event_id UUID NOT NULL REFERENCES outbox_events(id),
    channel VARCHAR(30) NOT NULL,
    destination VARCHAR(500) NOT NULL,
    delivered_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_notification_delivery UNIQUE (outbox_event_id, channel)
);

CREATE INDEX idx_notification_deliveries_event ON notification_deliveries (outbox_event_id);
