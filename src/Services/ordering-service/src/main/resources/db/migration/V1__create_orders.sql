CREATE TABLE orders (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL
);
