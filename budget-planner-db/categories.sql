INSERT INTO categories (name, icon)
VALUES ( 'Food', 'i-lucide-utensils'),
       ( 'Transport', 'i-lucide-bus'),
       ( 'Housing', 'i-lucide-home'),
       ( 'Entertainment', 'i-lucide-clapperboard'),
       ( 'Salary', 'i-lucide-banknote')
ON CONFLICT (name) DO NOTHING;
