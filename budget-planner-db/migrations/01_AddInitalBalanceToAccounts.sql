UPDATE accounts
SET initial_balance = balance
WHERE initial_balance = 0.00
  AND balance <> 0.00;
