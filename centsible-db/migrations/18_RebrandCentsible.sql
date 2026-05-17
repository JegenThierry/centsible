UPDATE system_information
SET name        = 'Centsible',
    modified_at = now()
WHERE id = 1
  AND name <> 'Centsible';

ALTER TABLE system_information
    ALTER COLUMN name SET DEFAULT 'Centsible';
