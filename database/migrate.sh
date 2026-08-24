#!/bin/sh
# Applies database/migrations/V*.sql in name order using psql.
# Connection comes from the usual PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD variables.
# Every file can be run again on a database that already has the objects.
set -eu
export PGOPTIONS="${PGOPTIONS:-} -c client_min_messages=warning"
dir=$(dirname "$0")/migrations
for f in "$dir"/V*.sql; do
    echo "applying $f"
    psql -v ON_ERROR_STOP=1 -q -f "$f"
done
