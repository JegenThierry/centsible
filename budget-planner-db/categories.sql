INSERT INTO categories (name, icon, color, type)
VALUES ('Food', 'i-lucide-utensils', '#ef4444', 'EXPENSE'),
       ('Transport', 'i-lucide-bus', '#3b82f6', 'EXPENSE'),
       ('Housing', 'i-lucide-home', '#eab308', 'EXPENSE'),
       ('Entertainment', 'i-lucide-clapperboard', '#a855f7', 'EXPENSE'),
       ('Salary', 'i-lucide-banknote', '#22c55e', 'INCOME')
ON CONFLICT (name) DO NOTHING;
