CREATE TABLE user_default_budget
(
    user_id    UUID                        NOT NULL,
    budget_id  UUID,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_user_default_budget PRIMARY KEY (user_id),
    CONSTRAINT fk_udb_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_udb_budget FOREIGN KEY (budget_id) REFERENCES budgets (id) ON DELETE SET NULL
);
