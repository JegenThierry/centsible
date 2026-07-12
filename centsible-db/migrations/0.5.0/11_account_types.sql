ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS type VARCHAR(20) NOT NULL DEFAULT 'CHECKING';

ALTER TABLE accounts
    DROP CONSTRAINT IF EXISTS accounts_type_check;

ALTER TABLE accounts
    ADD CONSTRAINT accounts_type_check
        CHECK (type IN (
            'CHECKING', 'SAVINGS', 'CASH', 'CREDIT_CARD',
            'INVESTMENT', 'ASSET', 'LOAN', 'MORTGAGE', 'OTHER'
        ));
