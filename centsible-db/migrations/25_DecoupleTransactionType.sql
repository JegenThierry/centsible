-- Decouple transaction type from category type.
-- transactions.type becomes the source of truth for sign; categories.type
-- now serves only as the suggested default when creating a transaction.

ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS type VARCHAR(10);

UPDATE transactions t
SET type = c.type
FROM categories c
WHERE t.category_id = c.id
  AND t.type IS NULL;

ALTER TABLE transactions
    ALTER COLUMN type SET NOT NULL;

ALTER TABLE transactions
    DROP CONSTRAINT IF EXISTS transactions_type_check;

ALTER TABLE transactions
    ADD CONSTRAINT transactions_type_check CHECK (type IN ('INCOME', 'EXPENSE'));

-- Rebuild account_history so the running balance derives from t.type, not c.type.
CREATE OR REPLACE VIEW account_history
AS
WITH history AS (SELECT a.id                                         AS account_id,
                        a.user_id,
                        a.initial_balance                            AS balance,
                        a.created_at                                 AS created_at,
                        '00000000-0000-0000-0000-000000000000'::uuid AS transaction_id
                 FROM accounts a
                 UNION ALL
                 SELECT t.account_id,
                        a.user_id,
                        a.initial_balance + SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END)
                                            OVER (PARTITION BY t.account_id ORDER BY t.transaction_date, t.created_at) AS balance,
                        t.created_at,
                        t.id                                                                                           AS transaction_id
                 FROM transactions t
                          JOIN accounts a ON t.account_id = a.id)
SELECT row_number() OVER (ORDER BY account_id, created_at) AS id,
       account_id,
       user_id,
       balance,
       created_at,
       transaction_id
FROM history;
