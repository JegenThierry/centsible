ALTER TABLE loans
    ADD COLUMN IF NOT EXISTS source_transaction_id UUID REFERENCES transactions (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_loans_source_transaction_id ON loans (source_transaction_id);
