CREATE TABLE IF NOT EXISTS categorization_rules
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    match_type  VARCHAR(16)  NOT NULL DEFAULT 'CONTAINS',
    pattern     VARCHAR(255) NOT NULL,
    category_id BIGINT       NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    priority    INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_categorization_rules_match_type
        CHECK (match_type IN ('CONTAINS', 'EQUALS', 'STARTS_WITH'))
);

CREATE INDEX IF NOT EXISTS idx_categorization_rules_user_priority
    ON categorization_rules (user_id, priority DESC, created_at);

CREATE UNIQUE INDEX IF NOT EXISTS uq_categorization_rules_user_pattern
    ON categorization_rules (user_id, lower(pattern), match_type);
