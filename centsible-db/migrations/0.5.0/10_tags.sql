CREATE TABLE IF NOT EXISTS tags
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(50) NOT NULL,
    color       VARCHAR(7)  NOT NULL DEFAULT '#6b7280',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tags_user ON tags (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_tags_user_name ON tags (user_id, lower(name));

CREATE TABLE IF NOT EXISTS transaction_tags
(
    transaction_id UUID   NOT NULL REFERENCES transactions (id) ON DELETE CASCADE,
    tag_id         BIGINT NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (transaction_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_transaction_tags_tag ON transaction_tags (tag_id);
