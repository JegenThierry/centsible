CREATE EXTENSION IF NOT EXISTS pgcrypto;

--------------------------------------------------------------------
-- Users
--------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    password_hash TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

--------------------------------------------------------------------
-- Accounts
--------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    balance     DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    currency    VARCHAR(3)    DEFAULT 'EUR',
    created_at  TIMESTAMPTZ   DEFAULT now(),
    modified_at TIMESTAMPTZ
);

--------------------------------------------------------------------
-- Categories
--------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(50)  NOT NULL,
    type       VARCHAR(10)  NOT NULL,
    icon       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_categories_user_lookup
    ON categories(user_id);

--------------------------------------------------------------------
-- Transactions
--------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id      BIGINT NOT NULL REFERENCES categories(id),
    account_id       UUID   NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    amount           DECIMAL(15,2) NOT NULL,
    description      TEXT,
    transaction_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at       TIMESTAMPTZ DEFAULT now(),
    modified_at      TIMESTAMPTZ
);
