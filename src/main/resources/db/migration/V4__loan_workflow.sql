CREATE TABLE loan_policies (
    id UUID PRIMARY KEY,
    role VARCHAR(20) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    max_active_loans INTEGER NOT NULL,
    duration_days INTEGER NOT NULL,
    CONSTRAINT uk_loan_policies_role_type UNIQUE (role, resource_type),
    CONSTRAINT ck_loan_policies_role CHECK (role IN ('STUDENT', 'STAFF', 'ADMIN')),
    CONSTRAINT ck_loan_policies_resource_type CHECK (
        resource_type IN ('BOOK', 'LAPTOP', 'EQUIPMENT', 'JOURNAL', 'DOCUMENT', 'OTHER')
    ),
    CONSTRAINT ck_loan_policies_limits CHECK (max_active_loans > 0 AND duration_days > 0)
);

INSERT INTO loan_policies (id, role, resource_type, max_active_loans, duration_days)
SELECT gen_random_uuid(), policy.role, resource_type.type, policy.max_loans, policy.duration
FROM (VALUES
    ('STUDENT', 3, 21),
    ('STAFF', 10, 30),
    ('ADMIN', 20, 60)
) AS policy(role, max_loans, duration)
CROSS JOIN (VALUES
    ('BOOK'), ('LAPTOP'), ('EQUIPMENT'), ('JOURNAL'), ('DOCUMENT'), ('OTHER')
) AS resource_type(type);

CREATE TABLE loans (
    id UUID PRIMARY KEY,
    borrower_id BIGINT NOT NULL,
    resource_item_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_at TIMESTAMP WITH TIME ZONE,
    borrowed_at TIMESTAMP WITH TIME ZONE,
    due_at TIMESTAMP WITH TIME ZONE,
    returned_at TIMESTAMP WITH TIME ZONE,
    approver_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_loans_borrower FOREIGN KEY (borrower_id) REFERENCES usuarios (id),
    CONSTRAINT fk_loans_resource_item FOREIGN KEY (resource_item_id) REFERENCES resource_items (id),
    CONSTRAINT fk_loans_approver FOREIGN KEY (approver_id) REFERENCES usuarios (id),
    CONSTRAINT ck_loans_status CHECK (
        status IN ('REQUESTED', 'APPROVED', 'ACTIVE', 'OVERDUE', 'RETURNED', 'CANCELLED', 'REJECTED')
    )
);

CREATE INDEX idx_loans_borrower_status ON loans (borrower_id, status);
CREATE INDEX idx_loans_item_status ON loans (resource_item_id, status);
CREATE INDEX idx_loans_due_at ON loans (due_at) WHERE status IN ('ACTIVE', 'OVERDUE');

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_id BIGINT,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    metadata VARCHAR(2000),
    CONSTRAINT fk_audit_events_actor FOREIGN KEY (actor_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_audit_events_entity ON audit_events (entity_type, entity_id);
CREATE INDEX idx_audit_events_occurred_at ON audit_events (occurred_at DESC);
