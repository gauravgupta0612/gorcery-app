#!/usr/bin/env bash
# Applies the migrations with psql (optional - the backend runs them automatically via Flyway on startup).
# Usage: PGPASSWORD=... ./apply_migrations.sh [host] [db] [user]
set -euo pipefail
HOST=${1:-localhost}; DB=${2:-gola_grocery}; USER=${3:-gola}
DIR="$(cd "$(dirname "$0")/../migrations" && pwd)"
for f in "$DIR"/V*__*.sql; do
  echo "Applying $(basename "$f")"
  psql -h "$HOST" -U "$USER" -d "$DB" -v ON_ERROR_STOP=1 -q -f "$f"
done
