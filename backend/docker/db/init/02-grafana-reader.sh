#!/usr/bin/env bash
set -euo pipefail

: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${GRAFANA_DB_PASSWORD:?GRAFANA_DB_PASSWORD is required}"

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v app_db="$POSTGRES_DB" \
  -v app_user="$POSTGRES_USER" \
  -v grafana_password="$GRAFANA_DB_PASSWORD" <<'EOSQL'
SELECT 'CREATE ROLE grafana_reader LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION'
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname = 'grafana_reader'
)
\gexec

ALTER ROLE grafana_reader WITH LOGIN PASSWORD :'grafana_password';
GRANT CONNECT ON DATABASE :"app_db" TO grafana_reader;
GRANT USAGE ON SCHEMA public TO grafana_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO grafana_reader;
ALTER DEFAULT PRIVILEGES FOR ROLE :"app_user" IN SCHEMA public
GRANT SELECT ON TABLES TO grafana_reader;
EOSQL
