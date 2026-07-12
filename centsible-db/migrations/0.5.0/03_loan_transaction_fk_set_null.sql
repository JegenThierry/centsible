ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_transaction_id_fkey;
ALTER TABLE loans
    ADD CONSTRAINT loans_transaction_id_fkey
        FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE SET NULL;

ALTER TABLE loan_repayments DROP CONSTRAINT IF EXISTS loan_repayments_transaction_id_fkey;
ALTER TABLE loan_repayments
    ADD CONSTRAINT loan_repayments_transaction_id_fkey
        FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE SET NULL;
