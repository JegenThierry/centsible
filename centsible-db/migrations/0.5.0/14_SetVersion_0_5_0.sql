UPDATE system_information
SET version     = '0.5.0',
    released_at = CURRENT_DATE,
    modified_at = now()
WHERE id = 1;
