ALTER TABLE outbox_events ADD exchange_name VARCHAR(128) NULL;

UPDATE outbox_events
SET exchange_name = 'microservices.events'
WHERE exchange_name IS NULL;

ALTER TABLE outbox_events ALTER COLUMN exchange_name VARCHAR(128) NOT NULL;
