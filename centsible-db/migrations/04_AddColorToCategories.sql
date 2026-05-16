-- Add color column to categories
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS color VARCHAR(7) NOT NULL DEFAULT '#3b82f6';

-- Update system categories to have specific colors
UPDATE categories
SET color = '#ef4444'
WHERE name = 'Food';
UPDATE categories
SET color = '#3b82f6'
WHERE name = 'Transport';
UPDATE categories
SET color = '#eab308'
WHERE name = 'Housing';
UPDATE categories
SET color = '#a855f7'
WHERE name = 'Entertainment';
UPDATE categories
SET color = '#22c55e'
WHERE name = 'Salary';
