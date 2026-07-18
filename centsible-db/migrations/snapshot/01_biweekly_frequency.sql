-- Allow the BIWEEKLY recurrence/schedule cadence (every two weeks) on both the recurring
-- transaction templates and the export schedules, which share the Frequency enum. The existing
-- CHECK constraints were defined inline, so their names are auto-generated; drop whichever CHECK
-- currently guards each frequency column by definition, then re-add the widened list.
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
