-- Saved per-user CSV mapping templates for the file-import wizard. When a user uploads a CSV,
-- the wizard suggests a CsvBankProfile (if any matches the header), the user tweaks the column
-- mapping if needed, and can optionally save the result here for reuse on the next upload.
--
-- source_profile_id + source_profile_version let the wizard warn "this bank changed its CSV
-- format since you saved this mapping" when a profile's version bumps.

CREATE TABLE IF NOT EXISTS import_mapping_templates
(
    id                      UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id                 UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name                    TEXT        NOT NULL,
    source_profile_id       TEXT,
    source_profile_version  INTEGER,
    mapping                 JSONB       NOT NULL,
    dialect                 JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_import_mapping_templates_user
    ON import_mapping_templates (user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_import_mapping_templates_user_name
    ON import_mapping_templates (user_id, lower(name));
