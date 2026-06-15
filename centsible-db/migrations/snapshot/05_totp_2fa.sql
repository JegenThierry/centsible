ALTER TABLE users
    ADD COLUMN IF NOT EXISTS totp_secret_encrypted         BYTEA,
    ADD COLUMN IF NOT EXISTS totp_pending_secret_encrypted BYTEA,
    ADD COLUMN IF NOT EXISTS totp_enabled                  BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS totp_last_used_step           BIGINT,
    ADD COLUMN IF NOT EXISTS totp_enabled_at               TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS mfa_pending_auth
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash BYTEA       NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_mfa_pending_auth_token_hash ON mfa_pending_auth (token_hash);
CREATE INDEX IF NOT EXISTS idx_mfa_pending_auth_user ON mfa_pending_auth (user_id);

CREATE TABLE IF NOT EXISTS user_recovery_codes
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash  TEXT        NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_recovery_codes_user ON user_recovery_codes (user_id);
