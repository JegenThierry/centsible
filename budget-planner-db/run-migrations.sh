#!/bin/bash
set -e

echo "Running database migrations..."

# Wait for database to be ready (if running from a separate container)
until psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -c '\q'; do
  echo "Database is unavailable - sleeping"
  sleep 1
done

# Create migrations table if it doesn't exist
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<EOF
CREATE TABLE IF NOT EXISTS schema_migrations (
    version TEXT PRIMARY KEY,
    executed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
EOF

for f in /migrations/*.sql; do
  if [ -f "$f" ]; then
    VERSION=$(basename "$f")
    # Check if migration has already been executed
    ALREADY_RUN=$(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -t -c "SELECT 1 FROM schema_migrations WHERE version = '$VERSION'" | xargs)
    
    if [ -z "$ALREADY_RUN" ]; then
        echo "Executing migration: $VERSION"
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f "$f"
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -c "INSERT INTO schema_migrations (version) VALUES ('$VERSION')"
    else
        echo "Skipping migration (already executed): $VERSION"
    fi
  fi
done

echo "All migrations executed successfully."
