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
