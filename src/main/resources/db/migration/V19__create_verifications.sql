-- Vérifications de caisse (billetage) : comptage physique du cash et comparaison au reste d'un budget.
-- Une vérification peut éventuellement déclencher la création d'un achat (manquant) ou
-- d'un crédit (excédent) ; les liens vers ces objets sont stockés pour traçabilité.

CREATE TABLE verifications
(
    id                                UUID                        NOT NULL,
    budget_id                         UUID                        NOT NULL,
    owner_id                          UUID                        NOT NULL,
    verification_date                 DATE                        NOT NULL,
    note                              TEXT,
    reference_rest                    NUMERIC(15, 2)              NOT NULL,
    counted_total                     NUMERIC(15, 2)              NOT NULL,
    difference                        NUMERIC(15, 2)              NOT NULL,
    regularization_kind               VARCHAR(16)                 NOT NULL DEFAULT 'NONE',
    regularization_purchase_id        UUID,
    regularization_credit_movement_id UUID,
    created_by                        UUID                        NOT NULL,
    modified_by                       UUID,
    created_at                        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified_at                       TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_verifications PRIMARY KEY (id),
    CONSTRAINT fk_verifications_budget FOREIGN KEY (budget_id)
        REFERENCES budgets (id) ON DELETE RESTRICT,
    CONSTRAINT fk_verifications_owner FOREIGN KEY (owner_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_verifications_created_by FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_verifications_purchase FOREIGN KEY (regularization_purchase_id)
        REFERENCES purchases (id) ON DELETE SET NULL,
    CONSTRAINT fk_verifications_credit_movement FOREIGN KEY (regularization_credit_movement_id)
        REFERENCES budget_movements (id) ON DELETE SET NULL,
    CONSTRAINT ck_verifications_regularization_kind CHECK (regularization_kind IN ('NONE', 'PURCHASE', 'CREDIT'))
);

CREATE INDEX idx_verifications_budget_date ON verifications (budget_id, verification_date DESC);
CREATE INDEX idx_verifications_owner ON verifications (owner_id);

CREATE TABLE verification_denominations
(
    verification_id    UUID NOT NULL,
    denomination_value INT  NOT NULL,
    quantity           INT  NOT NULL,
    CONSTRAINT pk_verification_denominations PRIMARY KEY (verification_id, denomination_value),
    CONSTRAINT fk_verification_denominations_verification FOREIGN KEY (verification_id)
        REFERENCES verifications (id) ON DELETE CASCADE,
    CONSTRAINT ck_verification_denominations_value CHECK (denomination_value > 0),
    CONSTRAINT ck_verification_denominations_quantity CHECK (quantity >= 0)
);
