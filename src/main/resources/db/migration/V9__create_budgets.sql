CREATE TABLE budgets
(
    id           UUID                        NOT NULL,
    name         VARCHAR(255)                NOT NULL,
    description  TEXT,
    initial_fund NUMERIC(15, 2)              NOT NULL,
    created_by   UUID                        NOT NULL,
    modified_by  UUID,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_budgets PRIMARY KEY (id),
    CONSTRAINT uk_budgets_name UNIQUE (name),
    CONSTRAINT fk_budgets_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_budgets_created_by ON budgets (created_by);

CREATE TABLE budget_editors
(
    budget_id UUID                        NOT NULL,
    user_id   UUID                        NOT NULL,
    added_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_budget_editors PRIMARY KEY (budget_id, user_id),
    CONSTRAINT fk_budget_editors_budget FOREIGN KEY (budget_id) REFERENCES budgets (id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_editors_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_budget_editors_user ON budget_editors (user_id);

ALTER TABLE purchases
    ADD COLUMN budget_id UUID,
    ADD CONSTRAINT fk_purchases_budget
        FOREIGN KEY (budget_id) REFERENCES budgets (id) ON DELETE SET NULL;

CREATE INDEX idx_purchases_budget ON purchases (budget_id);
