INSERT INTO categories (name, icon, color, type, is_managed, system_key)
VALUES ('Transfer out', 'i-lucide-arrow-up-right', '#06b6d4', 'EXPENSE', TRUE, 'TRANSFER_OUT'),
       ('Transfer in', 'i-lucide-arrow-down-left', '#06b6d4', 'INCOME', TRUE, 'TRANSFER_IN')
ON CONFLICT (name) WHERE user_id IS NULL DO NOTHING;
