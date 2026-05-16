-- Stores the user's preferred UI / email language.
-- Two-letter ISO-639 code constrained to the locales the app actually supports.
-- Backfilled with 'en' for existing rows; new rows default to 'en' so registration
-- works even when the client forgets to send a locale.
ALTER TABLE users
    ADD COLUMN locale CHAR(2) NOT NULL DEFAULT 'en';

ALTER TABLE users
    ADD CONSTRAINT users_locale_supported CHECK (locale IN ('en', 'fr', 'de'));
