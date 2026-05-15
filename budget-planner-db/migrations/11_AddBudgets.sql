CREATE TABLE IF NOT EXISTS budgets
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id      UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id  BIGINT         NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    amount_limit DECIMAL(15, 2) NOT NULL CHECK (amount_limit > 0),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modified_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_budgets_user_category UNIQUE (user_id, category_id)
);

CREATE INDEX IF NOT EXISTS idx_budgets_user_id ON budgets (user_id);
