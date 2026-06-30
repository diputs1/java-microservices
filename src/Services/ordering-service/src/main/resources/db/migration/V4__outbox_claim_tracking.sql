ALTER TABLE outbox_events ADD claimed_at DATETIME2 NULL;

CREATE INDEX ix_outbox_events_status_claimed_at_created_at
    ON outbox_events(status, claimed_at, created_at);
