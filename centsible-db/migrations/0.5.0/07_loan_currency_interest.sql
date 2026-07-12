ALTER TABLE loans
    ADD COLUMN IF NOT EXISTS currency      VARCHAR(3),
    ADD COLUMN IF NOT EXISTS interest_rate NUMERIC(5, 2);

UPDATE loans l
SET currency = a.currency
FROM transactions t
         JOIN accounts a ON a.id = t.account_id
WHERE l.transaction_id = t.id
  AND l.currency IS NULL;

UPDATE loans l
SET currency = u.default_currency
FROM users u
WHERE u.id = l.user_id
  AND l.currency IS NULL;

ALTER TABLE loans
    ALTER COLUMN currency SET DEFAULT 'EUR',
    ALTER COLUMN currency SET NOT NULL;

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
