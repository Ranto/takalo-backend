ALTER TABLE product_categories
    ADD CONSTRAINT uk_product_category_label UNIQUE (label);

ALTER TABLE product_categories
    ALTER COLUMN description TYPE VARCHAR(255) USING (description::VARCHAR(255));

ALTER TABLE product_categories
    ALTER COLUMN label TYPE VARCHAR(255) USING (label::VARCHAR(255));