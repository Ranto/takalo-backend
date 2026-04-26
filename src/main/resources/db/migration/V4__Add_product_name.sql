ALTER TABLE purchase_items
    ADD product_name VARCHAR(255);

ALTER TABLE purchase_items
    ALTER COLUMN product_name SET NOT NULL;