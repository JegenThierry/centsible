-- Categories used to share a global UNIQUE(name) constraint, which prevented
-- different users from naming their own categories the same thing. Make name
-- unique per user instead: one partial unique index for user-owned rows
-- (user_id IS NOT NULL) and another for system-owned rows (user_id IS NULL)
-- so the seeded global categories still can't collide on name.

ALTER TABLE categories DROP CONSTRAINT IF EXISTS uq_categories_name;

CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_name_per_user
    ON categories (user_id, name)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_name_system
    ON categories (name)
    WHERE user_id IS NULL;
