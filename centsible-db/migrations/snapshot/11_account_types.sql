-- Account types: classify each account (checking, savings, credit card, investment, ...).
-- Foundational for true net worth (asset vs liability) and future investment accounts.
-- Additive; existing accounts default to CHECKING. Mirrors api.model.budgetaccount.AccountType.

ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS type VARCHAR(20) NOT NULL DEFAULT 'CHECKING';

-- Drop-then-add keeps this idempotent across re-runs (ADD CONSTRAINT has no IF NOT EXISTS).
ALTER TABLE accounts
    DROP CONSTRAINT IF EXISTS accounts_type_check;

ALTER TABLE accounts
    ADD CONSTRAINT accounts_type_check
        CHECK (type IN (
            'CHECKING', 'SAVINGS', 'CASH', 'CREDIT_CARD',
            'INVESTMENT', 'ASSET', 'LOAN', 'MORTGAGE', 'OTHER'
        ));
