INSERT INTO categories (name, icon, type)
VALUES ( 'Food', 'i-lucide-utensils', 'EXPENSE'),
       ( 'Transport', 'i-lucide-bus', 'EXPENSE'),
       ( 'Housing', 'i-lucide-home', 'EXPENSE'),
       ( 'Entertainment', 'i-lucide-clapperboard', 'EXPENSE'),
       ( 'Salary', 'i-lucide-banknote', 'INCOME')
ON CONFLICT (name) DO NOTHING;
