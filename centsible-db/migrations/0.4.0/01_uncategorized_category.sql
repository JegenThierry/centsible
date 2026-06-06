INSERT INTO categories (name, icon, color, type, is_managed, system_key)
VALUES ('Uncategorized', 'i-lucide-circle-help', '#9ca3af', 'EXPENSE', FALSE, 'UNCATEGORIZED')
ON CONFLICT (name) WHERE user_id IS NULL DO NOTHING;
