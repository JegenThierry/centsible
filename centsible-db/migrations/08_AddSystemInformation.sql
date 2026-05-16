CREATE TABLE IF NOT EXISTS system_information
(
    id           INTEGER PRIMARY KEY              DEFAULT 1 CHECK (id = 1),
    name         VARCHAR(100)            NOT NULL DEFAULT 'Centsible',
    version      VARCHAR(50)             NOT NULL,
    description  TEXT,
    license      VARCHAR(50),
    repository   TEXT,
    released_at  DATE,
    created_at   TIMESTAMPTZ             NOT NULL DEFAULT now(),
    modified_at  TIMESTAMPTZ             NOT NULL DEFAULT now()
);

INSERT INTO system_information (id, name, version, description, license, repository, released_at)
VALUES (1,
        'Centsible',
        '0.0.1-SNAPSHOT',
        'A self-hosted personal budget planning application for tracking accounts, transactions, categories, and lending.',
        'AGPL-3.0',
        'https://codeberg.org/thierryjegen/budget-planner',
        CURRENT_DATE)
ON CONFLICT (id) DO NOTHING;
