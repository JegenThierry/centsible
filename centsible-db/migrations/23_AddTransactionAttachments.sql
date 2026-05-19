CREATE TABLE IF NOT EXISTS transaction_attachments
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    transaction_id  UUID         NOT NULL REFERENCES transactions (id) ON DELETE CASCADE,
    user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    filename        VARCHAR(255) NOT NULL,
    content_type    VARCHAR(127) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    storage_key     VARCHAR(255) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transaction_attachments_transaction
    ON transaction_attachments (transaction_id);

CREATE INDEX IF NOT EXISTS idx_transaction_attachments_user
    ON transaction_attachments (user_id);
