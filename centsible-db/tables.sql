DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'export_type') THEN
            CREATE TYPE export_type AS ENUM (
                'TRANSACTIONS',
                'LENDINGS_PER_CONTACT',
                'LENDINGS_ALL',
                'ACCOUNTS_SUMMARY'
                );
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'export_status') THEN
            CREATE TYPE export_status AS ENUM (
                'PENDING',
                'IN_PROGRESS',
                'COMPLETED',
                'FAILED'
                );
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'post_processing_type') THEN
            CREATE TYPE post_processing_type AS ENUM (
                'SEND_EMAIL'
                );
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'post_processing_status') THEN
            CREATE TYPE post_processing_status AS ENUM (
                'PENDING',
                'IN_PROGRESS',
                'COMPLETED',
                'FAILED'
                );
        END IF;
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


CREATE TABLE IF NOT EXISTS users
(
    id                              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    username                        VARCHAR(50)  NOT NULL UNIQUE,
    email                           VARCHAR(255) NOT NULL UNIQUE,
    first_name                      VARCHAR(100) NOT NULL,
    last_name                       VARCHAR(100) NOT NULL,
    password_hash                   TEXT         NOT NULL,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_at                     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    registered                      BOOLEAN      NOT NULL DEFAULT FALSE,
    registration_token              UUID,
    profile_picture                 TEXT,
    registration_token_hash         BYTEA,
    registration_token_expires_at   TIMESTAMPTZ,
    locale                          CHAR(2)      NOT NULL DEFAULT 'en',
    password_reset_token_hash       BYTEA,
    password_reset_token_expires_at TIMESTAMPTZ,
    notification_settings           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT users_locale_supported CHECK (locale IN ('en', 'fr', 'de'))
);

CREATE INDEX IF NOT EXISTS idx_users_registration_token_hash ON users (registration_token_hash);
CREATE INDEX IF NOT EXISTS idx_users_password_reset_token_hash ON users (password_reset_token_hash);


CREATE TABLE IF NOT EXISTS accounts
(
    id              UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id         UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name            VARCHAR(100)   NOT NULL,
    balance         DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    initial_balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency        VARCHAR(3)     NOT NULL DEFAULT 'EUR',
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modified_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts (user_id);


CREATE TABLE IF NOT EXISTS categories
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(50) NOT NULL,
    icon       VARCHAR(50) NOT NULL,
    color      VARCHAR(7)  NOT NULL DEFAULT '#3b82f6',
    type       VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_managed BOOLEAN     NOT NULL DEFAULT FALSE,
    system_key VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_categories_user_lookup ON categories (user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_name_per_user
    ON categories (user_id, name)
    WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_name_system
    ON categories (name)
    WHERE user_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_system_key
    ON categories (system_key)
    WHERE user_id IS NULL AND system_key IS NOT NULL;


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


CREATE TABLE IF NOT EXISTS recurring_transactions
(
    id          UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    account_id  UUID           NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    category_id BIGINT         NOT NULL REFERENCES categories (id),
    amount      DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    description TEXT           NOT NULL,
    frequency   VARCHAR(10)    NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY', 'YEARLY')),
    start_date  DATE           NOT NULL,
    end_date    DATE,
    next_run_at DATE           NOT NULL,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modified_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    original_amount   DECIMAL(15, 2),
    original_currency VARCHAR(3),
    CONSTRAINT chk_recurring_end_after_start CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_recurring_account_id ON recurring_transactions (account_id);
CREATE INDEX IF NOT EXISTS idx_recurring_due ON recurring_transactions (next_run_at) WHERE active = TRUE;


CREATE TABLE IF NOT EXISTS transactions
(
    id                       UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    category_id              BIGINT         NOT NULL REFERENCES categories (id),
    account_id               UUID           NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    amount                   DECIMAL(15, 2) NOT NULL,
    description              TEXT,
    transaction_date         DATE           NOT NULL DEFAULT CURRENT_DATE,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modified_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    recurring_transaction_id UUID           REFERENCES recurring_transactions (id) ON DELETE SET NULL,
    import_hash              VARCHAR(64),
    type                     VARCHAR(10)    NOT NULL,
    provider_connection_id   UUID           REFERENCES provider_connections (id) ON DELETE SET NULL,
    original_amount          DECIMAL(15, 2),
    original_currency        VARCHAR(3),
    exchange_rate            NUMERIC(20, 10),
    rate_date                DATE,
    CONSTRAINT transactions_type_check CHECK (type IN ('INCOME', 'EXPENSE'))
);

CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions (account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_category_id ON transactions (category_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions (transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_recurring_id ON transactions (recurring_transaction_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_transactions_account_import_hash
    ON transactions (account_id, import_hash)
    WHERE import_hash IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_transactions_account_type ON transactions (account_id, type);
CREATE INDEX IF NOT EXISTS idx_transactions_provider_connection_id
    ON transactions (provider_connection_id)
    WHERE provider_connection_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_transactions_account_date
    ON transactions (account_id, transaction_date, id);


CREATE TABLE IF NOT EXISTS exchange_rates
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    base_currency  VARCHAR(3)      NOT NULL,
    quote_currency VARCHAR(3)      NOT NULL,
    rate           NUMERIC(20, 10) NOT NULL CHECK (rate > 0),
    rate_date      DATE            NOT NULL,
    source         VARCHAR(32)     NOT NULL DEFAULT 'frankfurter',
    fetched_at     TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_exchange_rates_base_quote_date
    ON exchange_rates (base_currency, quote_currency, rate_date);
CREATE INDEX IF NOT EXISTS idx_exchange_rates_lookup
    ON exchange_rates (base_currency, quote_currency, rate_date DESC);


CREATE TABLE IF NOT EXISTS contacts
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100),
    picture     TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_contacts_id_user UNIQUE (id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_contacts_user_id ON contacts (user_id);


CREATE TABLE IF NOT EXISTS loans
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id        UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    contact_id     UUID           NOT NULL,
    transaction_id UUID UNIQUE REFERENCES transactions (id) ON DELETE CASCADE,
    lent_amount    DECIMAL(15, 2) NOT NULL CHECK (lent_amount > 0),
    owed_amount    DECIMAL(15, 2) NOT NULL CHECK (owed_amount >= 0),
    loan_date      DATE           NOT NULL DEFAULT CURRENT_DATE,
    description    TEXT,
    due_date       DATE,
    notes          TEXT,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modified_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_loans_contact_user FOREIGN KEY (contact_id, user_id)
        REFERENCES contacts (id, user_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_loans_user_contact ON loans (user_id, contact_id);
CREATE INDEX IF NOT EXISTS idx_loans_transaction_id ON loans (transaction_id);


CREATE TABLE IF NOT EXISTS loan_repayments
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    loan_id        UUID           NOT NULL REFERENCES loans (id) ON DELETE CASCADE,
    transaction_id UUID UNIQUE REFERENCES transactions (id) ON DELETE CASCADE,
    amount         DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    repaid_at      DATE           NOT NULL DEFAULT CURRENT_DATE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modified_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_loan_repayments_loan_id ON loan_repayments (loan_id);
CREATE INDEX IF NOT EXISTS idx_loan_repayments_transaction_id ON loan_repayments (transaction_id);


CREATE TABLE IF NOT EXISTS budgets
(
    id               UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id          UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id      BIGINT         NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    amount_limit     DECIMAL(15, 2) NOT NULL CHECK (amount_limit > 0),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modified_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    period_type      VARCHAR(16)    NOT NULL DEFAULT 'MONTHLY',
    rollover_enabled BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_budgets_user_category UNIQUE (user_id, category_id),
    CONSTRAINT chk_budgets_period_type CHECK (period_type IN ('MONTHLY', 'QUARTERLY', 'ANNUAL'))
);

CREATE INDEX IF NOT EXISTS idx_budgets_user_id ON budgets (user_id);


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


CREATE TABLE IF NOT EXISTS notifications
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(64)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    data       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    read_at    TIMESTAMPTZ           DEFAULT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications (user_id, read_at)
    WHERE read_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON notifications (user_id, created_at DESC);


CREATE TABLE IF NOT EXISTS transaction_attachments
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    transaction_id UUID         NOT NULL REFERENCES transactions (id) ON DELETE CASCADE,
    user_id        UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    filename       VARCHAR(255) NOT NULL,
    content_type   VARCHAR(127) NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    storage_key    VARCHAR(255) NOT NULL UNIQUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transaction_attachments_transaction
    ON transaction_attachments (transaction_id);
CREATE INDEX IF NOT EXISTS idx_transaction_attachments_user
    ON transaction_attachments (user_id);


CREATE TABLE IF NOT EXISTS export_jobs
(
    id            UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    user_id       UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type          export_type   NOT NULL,
    status        export_status NOT NULL DEFAULT 'PENDING',
    title         TEXT          NOT NULL,
    payload       BYTEA         NOT NULL,
    pdf           BYTEA,
    pdf_filename  TEXT,
    error_message TEXT,
    attempt_count INTEGER       NOT NULL DEFAULT 0,
    locked_at     TIMESTAMPTZ,
    locked_by     TEXT,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    modified_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_export_jobs_claimable
    ON export_jobs (status, created_at)
    WHERE status IN ('PENDING', 'IN_PROGRESS');
CREATE INDEX IF NOT EXISTS idx_export_jobs_user_created
    ON export_jobs (user_id, created_at DESC);


CREATE TABLE IF NOT EXISTS export_post_processing
(
    id            UUID PRIMARY KEY                DEFAULT gen_random_uuid(),
    export_job_id UUID                   NOT NULL REFERENCES export_jobs (id) ON DELETE CASCADE,
    type          post_processing_type   NOT NULL,
    status        post_processing_status NOT NULL DEFAULT 'PENDING',
    config        JSONB                  NOT NULL DEFAULT '{}'::jsonb,
    error_message TEXT,
    attempt_count INTEGER                NOT NULL DEFAULT 0,
    locked_at     TIMESTAMPTZ,
    locked_by     TEXT,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ            NOT NULL DEFAULT now(),
    modified_at   TIMESTAMPTZ            NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_export_post_processing_claimable
    ON export_post_processing (status, created_at)
    WHERE status IN ('PENDING', 'IN_PROGRESS');
CREATE INDEX IF NOT EXISTS idx_export_post_processing_job
    ON export_post_processing (export_job_id);


CREATE TABLE IF NOT EXISTS provider_connection_accounts
(
    id                     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    provider_connection_id UUID        NOT NULL REFERENCES provider_connections (id) ON DELETE CASCADE,
    account_id             UUID        REFERENCES accounts (id) ON DELETE SET NULL,
    external_account_id    TEXT        NOT NULL,
    external_metadata      JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (provider_connection_id, external_account_id)
);

CREATE INDEX IF NOT EXISTS idx_pca_account_id
    ON provider_connection_accounts (account_id)
    WHERE account_id IS NOT NULL;


CREATE TABLE IF NOT EXISTS import_mapping_templates
(
    id                     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id                UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name                   TEXT        NOT NULL,
    source_profile_id      TEXT,
    source_profile_version INTEGER,
    mapping                JSONB       NOT NULL,
    dialect                JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_import_mapping_templates_user
    ON import_mapping_templates (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_import_mapping_templates_user_name
    ON import_mapping_templates (user_id, lower(name));


CREATE TABLE IF NOT EXISTS system_information
(
    id          INTEGER PRIMARY KEY   DEFAULT 1 CHECK (id = 1),
    name        VARCHAR(100) NOT NULL DEFAULT 'Centsible',
    version     VARCHAR(50)  NOT NULL,
    description TEXT,
    license     VARCHAR(50),
    repository  TEXT,
    released_at DATE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);


CREATE TABLE IF NOT EXISTS schema_migrations
(
    version     TEXT PRIMARY KEY,
    executed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);


CREATE OR REPLACE VIEW account_history
AS
WITH history AS (SELECT a.id                                         AS account_id,
                        a.user_id,
                        a.initial_balance                            AS balance,
                        a.created_at                                 AS created_at,
                        '00000000-0000-0000-0000-000000000000'::uuid AS transaction_id
                 FROM accounts a
                 UNION ALL
                 SELECT t.account_id,
                        a.user_id,
                        a.initial_balance + SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END)
                                            OVER (PARTITION BY t.account_id ORDER BY t.transaction_date, t.created_at) AS balance,
                        t.created_at,
                        t.id                                                                                           AS transaction_id
                 FROM transactions t
                          JOIN accounts a ON t.account_id = a.id)
SELECT row_number() OVER (ORDER BY account_id, created_at) AS id,
       account_id,
       user_id,
       balance,
       created_at,
       transaction_id
FROM history;

CREATE OR REPLACE VIEW contact_balances AS
SELECT c.id                                         AS contact_id,
       c.user_id                                    AS user_id,
       COALESCE(SUM(l.lent_amount), 0)              AS total_lent,
       COALESCE(SUM(l.owed_amount), 0)              AS total_owed,
       COALESCE(SUM(rp.repaid_total), 0)            AS total_repaid,
       COALESCE(SUM(l.owed_amount), 0)
           - COALESCE(SUM(rp.repaid_total), 0)      AS outstanding,
       GREATEST(MAX(l.created_at), MAX(rp.last_at)) AS last_activity_at,
       COUNT(l.id) FILTER (
           WHERE COALESCE(rp.repaid_total, 0) < l.owed_amount
           )                                        AS open_loan_count
FROM contacts c
         LEFT JOIN loans l
                   ON l.contact_id = c.id
         LEFT JOIN (SELECT loan_id,
                           SUM(amount)    AS repaid_total,
                           MAX(repaid_at) AS last_at
                    FROM loan_repayments
                    GROUP BY loan_id) rp
                   ON rp.loan_id = l.id
GROUP BY c.id, c.user_id;


INSERT INTO categories (name, icon, color, type, is_managed, system_key)
VALUES ('Food', 'i-lucide-utensils', '#ef4444', 'EXPENSE', FALSE, NULL),
       ('Transport', 'i-lucide-bus', '#3b82f6', 'EXPENSE', FALSE, NULL),
       ('Housing', 'i-lucide-home', '#eab308', 'EXPENSE', FALSE, NULL),
       ('Entertainment', 'i-lucide-clapperboard', '#a855f7', 'EXPENSE', FALSE, NULL),
       ('Salary', 'i-lucide-banknote', '#22c55e', 'INCOME', FALSE, NULL),
       ('Stock Purchase', 'i-lucide-shopping-cart', '#0ea5e9', 'EXPENSE', FALSE, NULL),
       ('Stock Sale', 'i-lucide-circle-dollar-sign', '#f59e0b', 'INCOME', FALSE, NULL),
       ('Dividend', 'i-lucide-trending-up', '#8b5cf6', 'INCOME', FALSE, NULL),
       ('Stock Gift Received', 'i-lucide-gift', '#ec4899', 'INCOME', FALSE, NULL),
       ('Stock Gift Given', 'i-lucide-hand-heart', '#fb7185', 'EXPENSE', FALSE, NULL),
       ('Market Value Adjustment', 'i-lucide-scale', '#6366f1', 'INCOME', FALSE, 'BALANCE_ADJUSTMENT'),
       ('Interest', 'i-lucide-percent', '#14b8a6', 'INCOME', FALSE, NULL),
       ('Fees & Commissions', 'i-lucide-receipt', '#64748b', 'EXPENSE', FALSE, NULL),
       ('Taxes', 'i-lucide-landmark', '#dc2626', 'EXPENSE', FALSE, NULL),
       ('Savings Deposit', 'i-lucide-piggy-bank', '#22d3ee', 'EXPENSE', FALSE, NULL),
       ('Lending', 'i-lucide-hand-coins', '#f97316', 'EXPENSE', TRUE, NULL),
       ('Repayment', 'i-lucide-hand-helping', '#10b981', 'INCOME', TRUE, NULL),
       ('Uncategorized', 'i-lucide-circle-help', '#9ca3af', 'EXPENSE', FALSE, 'UNCATEGORIZED')
ON CONFLICT (name)
WHERE user_id IS NULL DO NOTHING;

INSERT INTO system_information (id, name, version, description, license, repository, released_at)
VALUES (1,
        'Centsible',
        '0.4.0',
        'A self-hosted personal budget planning application for tracking accounts, transactions, categories, and lending.',
        'AGPL-3.0',
        'https://codeberg.org/thierryjegen/budget-planner',
        CURRENT_DATE)
ON CONFLICT (id) DO NOTHING;
INSERT INTO schema_migrations (version) VALUES ('0.4.0/01_uncategorized_category.sql') ON CONFLICT (version) DO NOTHING;
INSERT INTO schema_migrations (version) VALUES ('0.4.0/02_categorization_rules.sql') ON CONFLICT (version) DO NOTHING;
INSERT INTO schema_migrations (version) VALUES ('0.4.0/03_multi_currency.sql') ON CONFLICT (version) DO NOTHING;
INSERT INTO schema_migrations (version) VALUES ('0.4.0/04_SetVersion_0_4_0.sql') ON CONFLICT (version) DO NOTHING;
