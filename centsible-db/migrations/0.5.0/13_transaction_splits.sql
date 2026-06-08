-- Split transactions: one transaction can be divided across several categories.
-- The transaction row stays the single anchor (account / date / amount / balance);
-- the per-category breakdown lives here. Reporting and budgets attribute amounts to
-- a split's category when splits exist, otherwise to the transaction's own category.
-- A transaction has either zero splits (simple) or >= 2 splits whose amounts sum to
-- the transaction amount; balances and account_history are unaffected by splitting.

CREATE TABLE IF NOT EXISTS transaction_splits
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    transaction_id UUID           NOT NULL REFERENCES transactions (id) ON DELETE CASCADE,
    category_id    BIGINT         NOT NULL REFERENCES categories (id),
    amount         DECIMAL(15, 2) NOT NULL,
    note           TEXT,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transaction_splits_transaction_id ON transaction_splits (transaction_id);
CREATE INDEX IF NOT EXISTS idx_transaction_splits_category_id ON transaction_splits (category_id);
