#!/bin/bash
set -e

echo "Running database migrations..."

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MIGRATIONS_DIR="${MIGRATIONS_DIR:-/migrations}"
[ -d "$MIGRATIONS_DIR" ] || MIGRATIONS_DIR="$SCRIPT_DIR/migrations"

until psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -c '\q'; do
  echo "Database is unavailable - sleeping"
  sleep 1
done

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<EOF
CREATE TABLE IF NOT EXISTS schema_migrations (
    version TEXT PRIMARY KEY,
    executed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
EOF

run_folder() {
  local dir="$1"
  [ -d "$dir" ] || return 0
  local folder
  folder="$(basename "$dir")"
  local f
  for f in "$dir"/*.sql; do
    [ -f "$f" ] || continue
    local version="$folder/$(basename "$f")"
    local already
    already=$(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -t \
      -c "SELECT 1 FROM schema_migrations WHERE version = '$version'" | xargs)
    if [ -z "$already" ]; then
      echo "Executing migration: $version"
      psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f "$f"
      psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
        -c "INSERT INTO schema_migrations (version) VALUES ('$version')"
    else
      echo "Skipping migration (already executed): $version"
    fi
  done
}

archives=$(
  find "$MIGRATIONS_DIR" -mindepth 1 -maxdepth 1 -type d ! -name snapshot 2>/dev/null \
  | while IFS= read -r d; do
      v="${d##*/}"
      IFS='.-' read -r ma mi pa _ <<< "$v"
      ma=${ma//[!0-9]/}; mi=${mi//[!0-9]/}; pa=${pa//[!0-9]/}
      printf '%010d.%010d.%010d\t%s\n' "$((10#${ma:-0}))" "$((10#${mi:-0}))" "$((10#${pa:-0}))" "$d"
    done \
  | sort \
  | cut -f2-
)
while IFS= read -r dir; do
  [ -n "$dir" ] && run_folder "$dir"
done <<< "$archives"

run_folder "$MIGRATIONS_DIR/snapshot"

echo "All migrations executed successfully."
