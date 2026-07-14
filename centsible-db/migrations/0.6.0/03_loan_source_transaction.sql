-- Split-to-IOUs: link a tracking-only loan back to the expense it was carved from.
-- Kept separate from loans.transaction_id (which marks a balance-affecting *lending* transaction
-- that is reversed when the loan is deleted); source_transaction_id is a soft back-reference only,
-- so deleting the source expense just unlinks its IOUs (SET NULL) rather than reversing anything.
ALTER TABLE loans
    ADD COLUMN IF NOT EXISTS source_transaction_id UUID REFERENCES transactions (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_loans_source_transaction_id ON loans (source_transaction_id);
