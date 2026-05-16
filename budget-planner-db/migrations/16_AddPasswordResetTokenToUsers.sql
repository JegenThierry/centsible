-- Hashed + expiring password reset tokens. The raw token is emailed to the user; only the
-- SHA-256 hash and expiry are stored. Mirrors the registration token columns added in 10_.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_reset_token_hash BYTEA;
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_reset_token_expires_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_password_reset_token_hash
    ON users (password_reset_token_hash);
