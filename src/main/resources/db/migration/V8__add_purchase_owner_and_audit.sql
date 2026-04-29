-- Utilisateur système servant de propriétaire/auditeur pour les achats existants
-- (avant l'introduction de owner_id / created_by sur purchases).
INSERT INTO users (id, external_id, email, display_name, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000999',
    'system',
    'system@takalo.local',
    'Système',
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
VALUES (
    '00000000-0000-0000-0000-000000000999',
    '00000000-0000-0000-0000-000000000001'
)
ON CONFLICT DO NOTHING;

-- Colonnes propriétaire (autorisation) + audit (traçabilité)
ALTER TABLE purchases
    ADD COLUMN owner_id     UUID,
    ADD COLUMN created_by   UUID,
    ADD COLUMN modified_by  UUID,
    ADD COLUMN created_at   TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN modified_at  TIMESTAMP WITHOUT TIME ZONE;

-- Backfill : tout achat antérieur est rattaché à l'utilisateur système.
UPDATE purchases
SET owner_id    = '00000000-0000-0000-0000-000000000999',
    created_by  = '00000000-0000-0000-0000-000000000999',
    modified_by = '00000000-0000-0000-0000-000000000999',
    created_at  = COALESCE(created_at, NOW()),
    modified_at = COALESCE(modified_at, NOW());

ALTER TABLE purchases
    ALTER COLUMN owner_id    SET NOT NULL,
    ALTER COLUMN created_by  SET NOT NULL,
    ALTER COLUMN created_at  SET NOT NULL;

ALTER TABLE purchases
    ADD CONSTRAINT fk_purchases_owner       FOREIGN KEY (owner_id)    REFERENCES users (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_purchases_created_by  FOREIGN KEY (created_by)  REFERENCES users (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_purchases_modified_by FOREIGN KEY (modified_by) REFERENCES users (id) ON DELETE RESTRICT;

CREATE INDEX idx_purchases_owner ON purchases (owner_id);
