-- Hashed + expiring registration tokens (was plaintext UUID, no expiry).
-- Old `registration_token` column kept until jooqCodegen is re-run; values are nulled here.
-- Pending unconfirmed registrations are invalidated by this migration.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS registration_token_hash BYTEA;
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS registration_token_expires_at TIMESTAMPTZ;

UPDATE users
SET registration_token = NULL
WHERE registration_token IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_registration_token_hash
    ON users (registration_token_hash);
