-- Postgres only pushes a qual through a windowed subquery when the qual's columns appear in every
-- window's PARTITION BY. The old view's outer row_number() had none, so every read scanned all users'
-- transactions; the inner window carried account_id but not user_id. Hence: no outer window, and
-- PARTITION BY (user_id, account_id) -- free, since an account belongs to exactly one user.
--
-- created_at must stay un-pushable: a running balance depends on every earlier transaction, so a date
-- range has to be filtered above the window, never inside it.
--
-- DROP + CREATE because CREATE OR REPLACE VIEW cannot remove the old synthetic `id` column.
DROP VIEW IF EXISTS account_history;

CREATE VIEW account_history AS
SELECT a.id                                         AS account_id,
       a.user_id,
       a.initial_balance                            AS balance,
       a.created_at                                 AS created_at,
       '00000000-0000-0000-0000-000000000000'::uuid AS transaction_id
FROM accounts a
UNION ALL
SELECT t.account_id,
       a.user_id,
       a.initial_balance + SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END)
                           OVER (PARTITION BY a.user_id, t.account_id ORDER BY t.transaction_date, t.created_at) AS balance,
       t.created_at,
       t.id                                                                                                      AS transaction_id
FROM transactions t
         JOIN accounts a ON t.account_id = a.id;
