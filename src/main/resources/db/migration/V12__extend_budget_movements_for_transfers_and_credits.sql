-- Source d'un mouvement de crédit (NULL pour les autres types).
-- Liste connue à ce stade : 'INCONNUE'. Étendable sans migration future.
ALTER TABLE budget_movements
    ADD COLUMN source VARCHAR(40);

-- Lien vers le budget de l'autre côté d'un transfert (NULL hors transferts).
ALTER TABLE budget_movements
    ADD COLUMN counterpart_budget_id UUID
        REFERENCES budgets (id) ON DELETE SET NULL;

CREATE INDEX idx_budget_movements_type ON budget_movements (budget_id, type);
