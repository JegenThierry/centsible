-- Account-activity timestamps for the admin area's staleness signals:
-- last_login_at is stamped on every successful login (password or 2FA);
-- last_seen_at is stamped from authenticated requests, throttled in SQL.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_seen_at  TIMESTAMPTZ;
