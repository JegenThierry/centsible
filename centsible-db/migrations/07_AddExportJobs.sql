DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'export_type') THEN
            CREATE TYPE export_type AS ENUM (
                'TRANSACTIONS',
                'LENDINGS_PER_CONTACT',
                'LENDINGS_ALL',
                'ACCOUNTS_SUMMARY'
                );
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'export_status') THEN
            CREATE TYPE export_status AS ENUM (
                'PENDING',
                'IN_PROGRESS',
                'COMPLETED',
                'FAILED'
                );
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'post_processing_type') THEN
            CREATE TYPE post_processing_type AS ENUM (
                'SEND_EMAIL'
                );
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'post_processing_status') THEN
            CREATE TYPE post_processing_status AS ENUM (
                'PENDING',
                'IN_PROGRESS',
                'COMPLETED',
                'FAILED'
                );
        END IF;
    END
$$;

CREATE TABLE IF NOT EXISTS export_jobs
(
    id            UUID PRIMARY KEY              DEFAULT gen_random_uuid(),
    user_id       UUID                 NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type          export_type          NOT NULL,
    status        export_status        NOT NULL DEFAULT 'PENDING',
    title         TEXT                 NOT NULL,
    payload       BYTEA                NOT NULL,
    pdf           BYTEA,
    pdf_filename  TEXT,
    error_message TEXT,
    attempt_count INTEGER              NOT NULL DEFAULT 0,
    locked_at     TIMESTAMPTZ,
    locked_by     TEXT,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ          NOT NULL DEFAULT now(),
    modified_at   TIMESTAMPTZ          NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_export_jobs_claimable
    ON export_jobs (status, created_at)
    WHERE status IN ('PENDING', 'IN_PROGRESS');

CREATE INDEX IF NOT EXISTS idx_export_jobs_user_created
    ON export_jobs (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS export_post_processing
(
    id            UUID PRIMARY KEY                 DEFAULT gen_random_uuid(),
    export_job_id UUID                    NOT NULL REFERENCES export_jobs (id) ON DELETE CASCADE,
    type          post_processing_type    NOT NULL,
    status        post_processing_status  NOT NULL DEFAULT 'PENDING',
    config        JSONB                   NOT NULL DEFAULT '{}'::jsonb,
    error_message TEXT,
    attempt_count INTEGER                 NOT NULL DEFAULT 0,
    locked_at     TIMESTAMPTZ,
    locked_by     TEXT,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ             NOT NULL DEFAULT now(),
    modified_at   TIMESTAMPTZ             NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_export_post_processing_claimable
    ON export_post_processing (status, created_at)
    WHERE status IN ('PENDING', 'IN_PROGRESS');

CREATE INDEX IF NOT EXISTS idx_export_post_processing_job
    ON export_post_processing (export_job_id);
