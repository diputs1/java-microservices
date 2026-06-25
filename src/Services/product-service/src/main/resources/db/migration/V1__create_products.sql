CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(18,2) NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku)
);
