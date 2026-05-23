-- Maps external provider account ids (e.g. GoCardless account UUID, PayPal merchant id) to
-- centsible accounts. Replaces the JSONB-encoded config.accountMap that v1 stored on
-- provider_connections — gives proper referential integrity, ON DELETE CASCADE semantics,
-- and lets the orchestrator query mappings without round-tripping through Jackson.
--
-- ON DELETE CASCADE on provider_connection_id: removing the connection drops its mappings.
-- ON DELETE SET NULL on account_id: removing a centsible account preserves the external_id
-- history so a subsequent re-sync can re-link (or surface the orphan to the user).

CREATE TABLE IF NOT EXISTS provider_connection_accounts
(
    id                     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    provider_connection_id UUID        NOT NULL REFERENCES provider_connections (id) ON DELETE CASCADE,
    account_id             UUID                 REFERENCES accounts (id) ON DELETE SET NULL,
    external_account_id    TEXT        NOT NULL,
    external_metadata      JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (provider_connection_id, external_account_id)
);

CREATE INDEX IF NOT EXISTS idx_pca_account_id
    ON provider_connection_accounts (account_id)
    WHERE account_id IS NOT NULL;
