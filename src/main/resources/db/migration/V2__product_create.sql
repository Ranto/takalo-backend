CREATE TABLE products
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    category_id UUID,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    deleted_at  TIMESTAMPTZ
);

ALTER TABLE products
    ADD CONSTRAINT fk_products_on_category FOREIGN KEY (category_id) REFERENCES product_categories (id);

ALTER TABLE products
    ADD CONSTRAINT uk_product_name UNIQUE (name);

CREATE INDEX idx_product_name ON products(name);
CREATE INDEX idx_product_category_id ON products(category_id);