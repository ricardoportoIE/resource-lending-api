CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(200) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES usuarios(id),
    endpoint VARCHAR(300) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    http_status INTEGER,
    response_body TEXT,
    content_type VARCHAR(150),
    location VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_scope UNIQUE (idempotency_key, user_id, endpoint),
    CONSTRAINT chk_idempotency_status CHECK (status IN ('PROCESSING', 'COMPLETED')),
    CONSTRAINT chk_idempotency_http_status CHECK (http_status IS NULL OR http_status BETWEEN 100 AND 599)
);

CREATE INDEX idx_idempotency_expiry ON idempotency_records (expires_at);
CREATE INDEX idx_idempotency_user_created ON idempotency_records (user_id, created_at DESC);
