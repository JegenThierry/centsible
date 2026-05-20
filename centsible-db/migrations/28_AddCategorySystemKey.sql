-- Adds a stable, locale-independent identifier for system categories so
-- callers can look one up without depending on the (English, user-editable)
-- display name. Currently used by the "set account balance" flow to find
-- its default adjustment category.

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS system_key VARCHAR(64);

-- One key per system row at most. User-owned categories cannot have a key.
CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_system_key
    ON categories (system_key)
    WHERE user_id IS NULL AND system_key IS NOT NULL;

-- Backfill the only currently-keyed system category. The investment seed
-- (migration 26) inserts 'Market Value Adjustment'; tag it here so existing
-- installs pick up the key without re-running the seed.
UPDATE categories
SET system_key = 'BALANCE_ADJUSTMENT'
WHERE user_id IS NULL
  AND name = 'Market Value Adjustment'
  AND system_key IS NULL;
