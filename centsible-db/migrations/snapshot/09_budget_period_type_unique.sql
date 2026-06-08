-- Allow one budget per (user, category, period_type) rather than one per (user, category), so a
-- single category can carry, e.g., both a MONTHLY and an ANNUAL budget at the same time. The old
-- (user, category) unique constraint made the period_type column effectively cosmetic.

ALTER TABLE budgets
    DROP CONSTRAINT IF EXISTS uq_budgets_user_category;

-- Drop-then-add keeps this idempotent across re-runs (ADD CONSTRAINT has no IF NOT EXISTS).
ALTER TABLE budgets
    DROP CONSTRAINT IF EXISTS uq_budgets_user_category_period;

ALTER TABLE budgets
    ADD CONSTRAINT uq_budgets_user_category_period UNIQUE (user_id, category_id, period_type);
