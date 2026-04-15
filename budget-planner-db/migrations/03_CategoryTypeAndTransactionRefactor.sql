-- Add type column to categories (if not already present)
ALTER TABLE categories ADD COLUMN IF NOT EXISTS type VARCHAR(10) CHECK (type IN ('INCOME', 'EXPENSE'));

-- Update existing categories to have a default type based on current transactions
-- (Only runs if transactions table still has a type column)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'transactions' AND column_name = 'type'
    ) THEN
        UPDATE categories c
        SET type = (
            SELECT t.type
            FROM transactions t
            WHERE t.category_id = c.id
            LIMIT 1
        )
        WHERE EXISTS (
            SELECT 1
            FROM transactions t
            WHERE t.category_id = c.id
        );
    END IF;
END $$;

-- Default categories (Salary as income, others as expense)
UPDATE categories SET type = 'INCOME' WHERE name = 'Salary' AND type IS NULL;
UPDATE categories SET type = 'EXPENSE' WHERE type IS NULL;

-- Make type column NOT NULL
ALTER TABLE categories ALTER COLUMN type SET NOT NULL;

-- Update account_history view
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
                        a.initial_balance + SUM(CASE WHEN c.type = 'INCOME' THEN t.amount ELSE -t.amount END)
                                            OVER (PARTITION BY t.account_id ORDER BY t.transaction_date, t.created_at) AS balance,
                        t.created_at,
                        t.id                                                                                           AS transaction_id
                 FROM transactions t
                          JOIN accounts a ON t.account_id = a.id
                          JOIN categories c ON t.category_id = c.id)
SELECT row_number() OVER (ORDER BY account_id, created_at) AS id,
       account_id,
       user_id,
       balance,
       created_at,
       transaction_id
FROM history;

-- Remove type column from transactions (if it exists)
ALTER TABLE transactions DROP COLUMN IF EXISTS type;
