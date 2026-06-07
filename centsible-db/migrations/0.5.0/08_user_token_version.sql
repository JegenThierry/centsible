-- Token versioning for "sign out everywhere". Every issued JWT embeds the user's current
-- token_version; bumping it (password change, 2FA disable, or an explicit sign-out-everywhere)
-- invalidates every previously issued token whose embedded version no longer matches. Existing
-- (pre-migration) tokens carry no version claim and are treated as version 0, so they keep working
-- until the first bump — at which point they too are invalidated.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;
