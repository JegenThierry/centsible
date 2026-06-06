INSERT INTO categories (name, icon, color, type, is_managed, system_key)
VALUES ('Transfer', 'i-lucide-arrow-right-left', '#06b6d4', 'EXPENSE', FALSE, NULL)
ON CONFLICT (name) WHERE user_id IS NULL DO NOTHING;
