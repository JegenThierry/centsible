-- Loans and loan repayments reference their backing transaction with ON DELETE CASCADE. Because the
-- lending/repayment transactions are ordinary, visible, deletable rows, deleting one from the normal
-- transactions UI cascades away the whole loan (and, in turn, all its repayments) with no balance
-- reversal -- silent, irreversible data loss. Switch both FKs to ON DELETE SET NULL so the loan ledger
-- survives; deletions that must reverse balances go through the loan service instead.

ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_transaction_id_fkey;
ALTER TABLE loans
    ADD CONSTRAINT loans_transaction_id_fkey
        FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE SET NULL;

ALTER TABLE loan_repayments DROP CONSTRAINT IF EXISTS loan_repayments_transaction_id_fkey;
ALTER TABLE loan_repayments
    ADD CONSTRAINT loan_repayments_transaction_id_fkey
        FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE SET NULL;
