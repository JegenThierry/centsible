-- Nullable FK from transactions to the provider connection that imported the row.
-- Nullable because:
--   * existing rows pre-date integrations
--   * manual entries via the UI never have one
--   * file-imports (CSV/OFX) never have one
-- ON DELETE SET NULL: removing a provider connection must NOT delete the transactions it
-- imported — it only severs the attribution.

ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS provider_connection_id UUID
        REFERENCES provider_connections (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_transactions_provider_connection_id
    ON transactions (provider_connection_id)
    WHERE provider_connection_id IS NOT NULL;
