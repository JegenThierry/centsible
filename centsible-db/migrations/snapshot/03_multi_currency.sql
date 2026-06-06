ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS original_amount   DECIMAL(15, 2),
    ADD COLUMN IF NOT EXISTS original_currency VARCHAR(3),
    ADD COLUMN IF NOT EXISTS exchange_rate     NUMERIC(20, 10),
    ADD COLUMN IF NOT EXISTS rate_date         DATE;

ALTER TABLE recurring_transactions
    ADD COLUMN IF NOT EXISTS original_amount   DECIMAL(15, 2),
    ADD COLUMN IF NOT EXISTS original_currency VARCHAR(3);

CREATE TABLE IF NOT EXISTS exchange_rates
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    base_currency  VARCHAR(3)      NOT NULL,
    quote_currency VARCHAR(3)      NOT NULL,
    rate           NUMERIC(20, 10) NOT NULL CHECK (rate > 0),
    rate_date      DATE            NOT NULL,
    source         VARCHAR(32)     NOT NULL DEFAULT 'frankfurter',
    fetched_at     TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_exchange_rates_base_quote_date
    ON exchange_rates (base_currency, quote_currency, rate_date);
CREATE INDEX IF NOT EXISTS idx_exchange_rates_lookup
    ON exchange_rates (base_currency, quote_currency, rate_date DESC);
