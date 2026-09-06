DROP TABLE IF EXISTS provider_connection_accounts;

DROP INDEX IF EXISTS idx_transactions_provider_connection_id;

ALTER TABLE transactions DROP COLUMN IF EXISTS provider_connection_id;

DROP TABLE IF EXISTS provider_connections;

DROP TYPE IF EXISTS provider_connection_status;
