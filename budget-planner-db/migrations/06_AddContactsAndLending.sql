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

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS is_managed BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO categories (name, icon, color, type, is_managed)
VALUES ('Lending', 'i-lucide-hand-coins', '#f97316', 'EXPENSE', TRUE),
       ('Repayment', 'i-lucide-hand-helping', '#10b981', 'INCOME', TRUE)
ON CONFLICT (name) DO UPDATE
    SET icon       = EXCLUDED.icon,
        color      = EXCLUDED.color,
        type       = EXCLUDED.type,
        is_managed = EXCLUDED.is_managed
    WHERE categories.user_id IS NULL;

CREATE TABLE IF NOT EXISTS loans
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id        UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    contact_id     UUID           NOT NULL,
    transaction_id UUID           UNIQUE REFERENCES transactions (id) ON DELETE CASCADE,
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
    transaction_id UUID           UNIQUE REFERENCES transactions (id) ON DELETE CASCADE,
    amount         DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    repaid_at      DATE           NOT NULL DEFAULT CURRENT_DATE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modified_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_loan_repayments_loan_id ON loan_repayments (loan_id);
CREATE INDEX IF NOT EXISTS idx_loan_repayments_transaction_id ON loan_repayments (transaction_id);

CREATE OR REPLACE VIEW contact_balances AS
SELECT c.id                                          AS contact_id,
       c.user_id                                     AS user_id,
       COALESCE(SUM(l.lent_amount), 0)               AS total_lent,
       COALESCE(SUM(l.owed_amount), 0)               AS total_owed,
       COALESCE(SUM(rp.repaid_total), 0)             AS total_repaid,
       COALESCE(SUM(l.owed_amount), 0)
           - COALESCE(SUM(rp.repaid_total), 0)       AS outstanding,
       GREATEST(MAX(l.created_at), MAX(rp.last_at))  AS last_activity_at,
       COUNT(l.id) FILTER (
           WHERE COALESCE(rp.repaid_total, 0) < l.owed_amount
           )                                         AS open_loan_count
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
