ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS import_hash VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_transactions_account_import_hash
    ON transactions (account_id, import_hash)
    WHERE import_hash IS NOT NULL;
