CREATE TABLE budget_movements
(
    id             UUID                        NOT NULL,
    budget_id      UUID                        NOT NULL,
    type           VARCHAR(40)                 NOT NULL,
    amount         NUMERIC(15, 2)              NOT NULL,
    occurred_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reason         TEXT                        NOT NULL,
    correlation_id UUID,
    purchase_id    UUID,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by     UUID                        NOT NULL,
    CONSTRAINT pk_budget_movements PRIMARY KEY (id),
    CONSTRAINT fk_budget_movements_budget FOREIGN KEY (budget_id) REFERENCES budgets (id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_movements_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id) ON DELETE SET NULL,
    CONSTRAINT fk_budget_movements_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_budget_movements_budget ON budget_movements (budget_id, occurred_at);
CREATE INDEX idx_budget_movements_correlation ON budget_movements (correlation_id);
