CREATE TABLE IF NOT EXISTS recurring_transactions
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    account_id   UUID           NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    category_id  BIGINT         NOT NULL REFERENCES categories (id),
    amount       DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    description  TEXT           NOT NULL,
    frequency    VARCHAR(10)    NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    start_date   DATE           NOT NULL,
    end_date     DATE,
    next_run_at  DATE           NOT NULL,
    active       BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modified_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT chk_recurring_end_after_start CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_recurring_account_id ON recurring_transactions (account_id);
CREATE INDEX IF NOT EXISTS idx_recurring_due ON recurring_transactions (next_run_at) WHERE active = TRUE;

ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS recurring_transaction_id UUID REFERENCES recurring_transactions (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_transactions_recurring_id ON transactions (recurring_transaction_id);
