-- Reports/aggregates introduced in migration 25 now filter and group on
-- transactions.type after the column was extracted from categories. The
-- existing single-column index on account_id alone is not selective enough
-- for queries that also constrain by type (e.g. expense-by-category, cash
-- flow). A composite index lines up with the new query shape.

CREATE INDEX IF NOT EXISTS idx_transactions_account_type
    ON transactions (account_id, type);
