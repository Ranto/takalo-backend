CREATE TABLE purchase_items
(
    id          UUID             NOT NULL,
    purchase_id UUID             NOT NULL,
    product_id  UUID             NOT NULL,
    unit_price  DECIMAL(10, 2)   NOT NULL,
    discount    DECIMAL(10, 2)   NOT NULL,
    quantity    DOUBLE PRECISION NOT NULL,
    expiry_date date,
    store_name  VARCHAR(255),
    CONSTRAINT pk_purchase_items PRIMARY KEY (id)
);

CREATE TABLE purchases
(
    id            UUID                        NOT NULL,
    purchase_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_purchases PRIMARY KEY (id)
);

CREATE INDEX idx_purchase_date ON purchases (purchase_date);

CREATE INDEX idx_purchase_item_expiry_date ON purchase_items (expiry_date);

CREATE INDEX idx_purchase_item_product ON purchase_items (product_id);

ALTER TABLE purchase_items
    ADD CONSTRAINT FK_PURCHASE_ITEMS_ON_PURCHASE FOREIGN KEY (purchase_id) REFERENCES purchases (id);

CREATE INDEX idx_purchase_item_purchase ON purchase_items (purchase_id);