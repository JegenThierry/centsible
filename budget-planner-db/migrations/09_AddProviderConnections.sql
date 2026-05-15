DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'provider_connection_status') THEN
            CREATE TYPE provider_connection_status AS ENUM (
                'NEW',
                'ACTIVE',
                'ERROR',
                'REVOKED'
                );
        END IF;
    END
$$;

CREATE TABLE IF NOT EXISTS provider_connections
(
    id               UUID PRIMARY KEY                    DEFAULT gen_random_uuid(),
    user_id          UUID                       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider_key     TEXT                       NOT NULL,
    display_name     TEXT                       NOT NULL,
    status           provider_connection_status NOT NULL DEFAULT 'NEW',
    config           JSONB                      NOT NULL DEFAULT '{}'::jsonb,
    credentials      BYTEA,
    last_sync_at     TIMESTAMPTZ,
    last_sync_cursor TEXT,
    last_error       TEXT,
    attempt_count    INTEGER                    NOT NULL DEFAULT 0,
    locked_at        TIMESTAMPTZ,
    locked_by        TEXT,
    created_at       TIMESTAMPTZ                NOT NULL DEFAULT now(),
    modified_at      TIMESTAMPTZ                NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_provider_connections_user
    ON provider_connections (user_id);

CREATE INDEX IF NOT EXISTS idx_provider_connections_claimable
    ON provider_connections (status, last_sync_at)
    WHERE status = 'ACTIVE';
