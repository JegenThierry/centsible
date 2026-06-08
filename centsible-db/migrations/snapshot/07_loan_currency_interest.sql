-- Loan-level currency + optional interest rate.
--
-- currency: every loan is now denominated in its own currency (mirrors
--   api.model.budgetaccount.Currency, accounts.currency and users.default_currency).
--   Balance-affecting loans in a foreign currency convert to the account currency on the
--   linked transaction (original_amount/exchange_rate live on transactions); tracking-only
--   loans simply carry their currency.
-- interest_rate: optional (NULL = no interest). When set, owed = lent * (1 + rate/100),
--   computed once at creation. Stored as a percentage (e.g. 5.25 = 5.25%).

ALTER TABLE loans
    ADD COLUMN IF NOT EXISTS currency      VARCHAR(3),
    ADD COLUMN IF NOT EXISTS interest_rate NUMERIC(5, 2);

-- Backfill currency for pre-existing loans:
--   balance-affecting loans take the linked transaction's account currency,
UPDATE loans l
SET currency = a.currency
FROM transactions t
         JOIN accounts a ON a.id = t.account_id
WHERE l.transaction_id = t.id
  AND l.currency IS NULL;

--   tracking-only loans fall back to the owner's default currency.
UPDATE loans l
SET currency = u.default_currency
FROM users u
WHERE u.id = l.user_id
  AND l.currency IS NULL;

ALTER TABLE loans
    ALTER COLUMN currency SET DEFAULT 'EUR',
    ALTER COLUMN currency SET NOT NULL;

-- Drop-then-add keeps these idempotent across re-runs (ADD CONSTRAINT has no IF NOT EXISTS).
ALTER TABLE loans
    DROP CONSTRAINT IF EXISTS loans_currency_supported;
ALTER TABLE loans
    ADD CONSTRAINT loans_currency_supported
        CHECK (currency IN (
            'EUR', 'USD', 'JPY', 'GBP', 'AUD', 'CAD', 'CHF', 'CNY', 'HKD', 'NZD',
            'SEK', 'NOK', 'DKK', 'SGD', 'KRW', 'INR', 'MXN', 'BRL', 'ZAR', 'TRY',
            'PLN', 'PHP', 'IDR'
        ));

ALTER TABLE loans
    DROP CONSTRAINT IF EXISTS loans_interest_rate_non_negative;
ALTER TABLE loans
    ADD CONSTRAINT loans_interest_rate_non_negative
        CHECK (interest_rate IS NULL OR interest_rate >= 0);
