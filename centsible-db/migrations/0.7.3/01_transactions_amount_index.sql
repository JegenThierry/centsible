-- Serves TransactionRepository's AMOUNT_DESC / AMOUNT_ASC sorts. Column order mirrors
-- idx_transactions_account_date: leading account_id equality drives the scan, (amount, id) supplies
-- the ordering in both directions.
CREATE INDEX IF NOT EXISTS idx_transactions_account_amount
    ON transactions (account_id, amount, id);
