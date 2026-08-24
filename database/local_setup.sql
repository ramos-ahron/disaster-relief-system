-- Local development only. Run once as a PostgreSQL superuser:
--   psql -U postgres -f database/local_setup.sql
-- Creates the role and database the app uses by default (see Database.fromEnvironment).
-- Does nothing if they already exist.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'oop') THEN
        CREATE ROLE oop LOGIN PASSWORD 'ucalgary';
    END IF;
END
$$;
SELECT 'CREATE DATABASE ensf380project OWNER oop'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ensf380project') \gexec
