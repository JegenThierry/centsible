-- The Currency enum previously contained the invalid code 'YEN'. The correct
-- ISO 4217 code for the Japanese Yen is 'JPY', which is what java.util.Currency
-- (used by the export renderer) recognises. Rewrite any existing rows.
UPDATE accounts
SET currency    = 'JPY',
    modified_at = now()
WHERE currency = 'YEN';
