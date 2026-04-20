CREATE TABLE product_categories (
    id uuid primary key,
    label varchar(100) not null,
    description TEXT
);

CREATE INDEX idx_product_category_label ON product_categories(label);