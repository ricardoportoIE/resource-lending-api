CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    resource_id UUID NOT NULL,
    ready_item_id UUID,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ready_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_reservations_user FOREIGN KEY (user_id) REFERENCES usuarios (id),
    CONSTRAINT fk_reservations_resource FOREIGN KEY (resource_id) REFERENCES resources (id),
    CONSTRAINT fk_reservations_ready_item FOREIGN KEY (ready_item_id) REFERENCES resource_items (id),
    CONSTRAINT ck_reservations_status CHECK (
        status IN ('WAITING', 'READY', 'FULFILLED', 'EXPIRED', 'CANCELLED')
    )
);

CREATE INDEX idx_reservations_resource_queue
    ON reservations (resource_id, status, created_at);
CREATE INDEX idx_reservations_user_status
    ON reservations (user_id, status);
CREATE UNIQUE INDEX uk_reservations_user_resource_open
    ON reservations (user_id, resource_id)
    WHERE status IN ('WAITING', 'READY');

CREATE UNIQUE INDEX uk_loans_item_open
    ON loans (resource_item_id)
    WHERE status IN ('REQUESTED', 'APPROVED', 'ACTIVE', 'OVERDUE');
