-- Saved transaction filter views: a named, user-scoped snapshot of the transaction filter set,
-- stored as a JSONB payload mirroring the api TransactionFilters DTO so re-applying a view is a
-- straight deserialize. Owner-scoped like every other user-owned table (ADR-0003).
CREATE TABLE IF NOT EXISTS saved_transaction_filters
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    filters     JSONB        NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_saved_transaction_filters_user
    ON saved_transaction_filters (user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_saved_transaction_filters_user_name
    ON saved_transaction_filters (user_id, lower(name));
