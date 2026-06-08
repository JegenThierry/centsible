-- Generalize the single-trigger/single-action categorization_rules into a rule engine:
-- a rule has N conditions (matched ALL or ANY) and N actions (set category, add a tag).
-- Existing categorization rules are migrated 1:1 (one DESCRIPTION condition + one SET_CATEGORY action).

CREATE TABLE IF NOT EXISTS rules
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    match_all   BOOLEAN      NOT NULL DEFAULT TRUE,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    priority    INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_rules_user_priority
    ON rules (user_id, priority DESC, created_at);

CREATE TABLE IF NOT EXISTS rule_conditions
(
    id       UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    rule_id  UUID         NOT NULL REFERENCES rules (id) ON DELETE CASCADE,
    field    VARCHAR(20)  NOT NULL,
    operator VARCHAR(20)  NOT NULL,
    value    VARCHAR(255) NOT NULL,
    CONSTRAINT chk_rule_conditions_field
        CHECK (field IN ('DESCRIPTION', 'AMOUNT', 'DIRECTION', 'ACCOUNT')),
    CONSTRAINT chk_rule_conditions_operator
        CHECK (operator IN ('CONTAINS', 'EQUALS', 'STARTS_WITH', 'GT', 'GTE', 'LT', 'LTE', 'IS'))
);

CREATE INDEX IF NOT EXISTS idx_rule_conditions_rule ON rule_conditions (rule_id);

CREATE TABLE IF NOT EXISTS rule_actions
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id     UUID        NOT NULL REFERENCES rules (id) ON DELETE CASCADE,
    action_type VARCHAR(20) NOT NULL,
    category_id BIGINT REFERENCES categories (id) ON DELETE CASCADE,
    tag_id      BIGINT REFERENCES tags (id) ON DELETE CASCADE,
    CONSTRAINT chk_rule_actions_type
        CHECK (action_type IN ('SET_CATEGORY', 'ADD_TAG')),
    CONSTRAINT chk_rule_actions_target CHECK (
        (action_type = 'SET_CATEGORY' AND category_id IS NOT NULL AND tag_id IS NULL) OR
        (action_type = 'ADD_TAG' AND tag_id IS NOT NULL AND category_id IS NULL)
        )
);

CREATE INDEX IF NOT EXISTS idx_rule_actions_rule ON rule_actions (rule_id);

-- One-time migration of existing categorization_rules into the new structure, then drop the old table.
-- A transactional DO block so a partial failure rolls back cleanly and the file can safely re-run.
DO
$$
    BEGIN
        IF EXISTS (SELECT FROM information_schema.tables
                   WHERE table_schema = 'public' AND table_name = 'categorization_rules') THEN

            INSERT INTO rules (id, user_id, name, match_all, enabled, priority, created_at, modified_at)
            SELECT id, user_id, left(pattern, 100), TRUE, TRUE, priority, created_at, modified_at
            FROM categorization_rules
            ON CONFLICT (id) DO NOTHING;

            INSERT INTO rule_conditions (rule_id, field, operator, value)
            SELECT id, 'DESCRIPTION', match_type, pattern
            FROM categorization_rules;

            INSERT INTO rule_actions (rule_id, action_type, category_id)
            SELECT id, 'SET_CATEGORY', category_id
            FROM categorization_rules;

            DROP TABLE categorization_rules;
        END IF;
    END
$$;
