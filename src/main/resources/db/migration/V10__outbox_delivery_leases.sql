ALTER TABLE outbox_events
    ADD COLUMN processing_token UUID;

ALTER TABLE outbox_events
    DROP CONSTRAINT chk_outbox_status;

ALTER TABLE outbox_events
    ADD CONSTRAINT chk_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));
