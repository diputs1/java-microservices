ALTER TABLE orders ADD idempotency_key VARCHAR(128) NULL;

CREATE UNIQUE INDEX ux_orders_customer_idempotency_key
    ON orders(customer_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE order_items (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    sku VARCHAR(128) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    line_amount DECIMAL(18,2) NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX ix_order_items_order_id ON order_items(order_id);

CREATE TABLE outbox_events (
    event_id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    payload NVARCHAR(MAX) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INT NOT NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME2 NOT NULL,
    published_at DATETIME2 NULL
);

CREATE INDEX ix_outbox_events_status_created_at
    ON outbox_events(status, created_at);
