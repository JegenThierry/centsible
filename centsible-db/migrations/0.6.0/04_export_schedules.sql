-- Scheduled/automated export delivery: a recurring export that a materializer enqueues as an
-- ordinary export_jobs row each period (mirrors recurring_transactions). No changes to export_jobs
-- or the proto contract — schedules just mint normal jobs with a SEND_EMAIL post-processor.
CREATE TABLE IF NOT EXISTS export_schedules
(
    id          UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        export_type   NOT NULL DEFAULT 'TRANSACTIONS',
    format      VARCHAR(10)   NOT NULL DEFAULT 'PDF' CHECK (format IN ('PDF', 'CSV', 'JSON')),
    title       TEXT          NOT NULL,
    frequency   VARCHAR(10)   NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    next_run_at DATE          NOT NULL,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    last_run_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    modified_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_export_schedules_due ON export_schedules (next_run_at) WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_export_schedules_user ON export_schedules (user_id);
