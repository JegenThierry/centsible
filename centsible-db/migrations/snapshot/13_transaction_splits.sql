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
