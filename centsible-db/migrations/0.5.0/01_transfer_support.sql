ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS transfer_group_id UUID;

CREATE INDEX IF NOT EXISTS idx_transactions_transfer_group
    ON transactions (transfer_group_id)
    WHERE transfer_group_id IS NOT NULL;

ALTER TABLE recurring_transactions
    ALTER COLUMN category_id DROP NOT NULL;

ALTER TABLE recurring_transactions
    ADD COLUMN IF NOT EXISTS is_transfer            BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS destination_account_id UUID REFERENCES accounts (id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS type                   VARCHAR(10);

ALTER TABLE recurring_transactions
    DROP CONSTRAINT IF EXISTS chk_recurring_type;
ALTER TABLE recurring_transactions
    ADD CONSTRAINT chk_recurring_type CHECK (type IS NULL OR type IN ('INCOME', 'EXPENSE'));

ALTER TABLE recurring_transactions
    DROP CONSTRAINT IF EXISTS chk_recurring_transfer;
ALTER TABLE recurring_transactions
    ADD CONSTRAINT chk_recurring_transfer CHECK (
        (is_transfer = FALSE AND category_id IS NOT NULL AND destination_account_id IS NULL)
            OR (is_transfer = TRUE AND destination_account_id IS NOT NULL AND destination_account_id <> account_id)
        );

CREATE INDEX IF NOT EXISTS idx_recurring_destination_account
    ON recurring_transactions (destination_account_id)
    WHERE destination_account_id IS NOT NULL;
