-- Per-user notification preferences and thresholds. Storing as JSONB keeps the schema flat while
-- allowing new alert types to be added without further migrations.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS notification_settings JSONB NOT NULL DEFAULT '{}'::jsonb;
