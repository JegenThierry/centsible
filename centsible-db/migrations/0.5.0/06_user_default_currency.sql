ALTER TABLE users
    ADD COLUMN IF NOT EXISTS default_currency VARCHAR(3) NOT NULL DEFAULT 'EUR';

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_default_currency_supported;

ALTER TABLE users
    ADD CONSTRAINT users_default_currency_supported
        CHECK (default_currency IN (
            'EUR', 'USD', 'JPY', 'GBP', 'AUD', 'CAD', 'CHF', 'CNY', 'HKD', 'NZD',
            'SEK', 'NOK', 'DKK', 'SGD', 'KRW', 'INR', 'MXN', 'BRL', 'ZAR', 'TRY',
            'PLN', 'PHP', 'IDR'
        ));
