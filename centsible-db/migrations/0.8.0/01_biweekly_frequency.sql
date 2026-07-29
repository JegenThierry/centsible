DO $$
DECLARE
    c record;
BEGIN
    FOR c IN
        SELECT conrelid::regclass AS tbl, conname
        FROM pg_constraint
        WHERE contype = 'c'
          AND conrelid IN ('recurring_transactions'::regclass, 'export_schedules'::regclass)
          AND pg_get_constraintdef(oid) ILIKE '%frequency%'
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', c.tbl, c.conname);
    END LOOP;
END $$;

ALTER TABLE recurring_transactions
    ADD CONSTRAINT recurring_transactions_frequency_check
    CHECK (frequency IN ('DAILY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY', 'YEARLY'));

ALTER TABLE export_schedules
    ADD CONSTRAINT export_schedules_frequency_check
    CHECK (frequency IN ('DAILY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY', 'YEARLY'));
