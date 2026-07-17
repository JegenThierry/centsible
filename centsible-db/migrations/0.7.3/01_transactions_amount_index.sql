CREATE INDEX IF NOT EXISTS idx_transactions_account_amount
    ON transactions (account_id, amount, id);
