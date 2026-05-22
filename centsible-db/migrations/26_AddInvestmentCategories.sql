-- Default system categories oriented around stock / investment workflows.
-- Users can override the type per-transaction (see migration 25); these
-- entries set the suggested default and provide consistent iconography.
INSERT INTO categories (name, icon, color, type)
VALUES ('Stock Purchase', 'i-lucide-shopping-cart', '#0ea5e9', 'EXPENSE'),
       ('Stock Sale', 'i-lucide-circle-dollar-sign', '#f59e0b', 'INCOME'),
       ('Dividend', 'i-lucide-trending-up', '#8b5cf6', 'INCOME'),
       ('Stock Gift Received', 'i-lucide-gift', '#ec4899', 'INCOME'),
       ('Stock Gift Given', 'i-lucide-hand-heart', '#fb7185', 'EXPENSE'),
       ('Market Value Adjustment', 'i-lucide-scale', '#6366f1', 'INCOME'),
       ('Interest', 'i-lucide-percent', '#14b8a6', 'INCOME'),
       ('Fees & Commissions', 'i-lucide-receipt', '#64748b', 'EXPENSE'),
       ('Taxes', 'i-lucide-landmark', '#dc2626', 'EXPENSE'),
       ('Savings Deposit', 'i-lucide-piggy-bank', '#22d3ee', 'EXPENSE')
ON CONFLICT (name) WHERE user_id IS NULL DO NOTHING;
