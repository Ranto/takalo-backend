-- Verrouillage d'un achat : une fois verrouillé, seul le SUPER_ADMIN peut le modifier,
-- le supprimer ou réassigner son budget. Le déverrouillage est également réservé au SUPER_ADMIN.
ALTER TABLE purchases
    ADD COLUMN locked_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN locked_by UUID;

ALTER TABLE purchases
    ADD CONSTRAINT fk_purchases_locked_by FOREIGN KEY (locked_by) REFERENCES users (id) ON DELETE RESTRICT;

CREATE INDEX idx_purchases_locked_at ON purchases (locked_at);
