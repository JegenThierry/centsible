ALTER TABLE budgets
    DROP CONSTRAINT IF EXISTS uq_budgets_user_category;

ALTER TABLE budgets
    DROP CONSTRAINT IF EXISTS uq_budgets_user_category_period;

ALTER TABLE budgets
    ADD CONSTRAINT uq_budgets_user_category_period UNIQUE (user_id, category_id, period_type);
