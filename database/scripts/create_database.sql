-- Run once as a PostgreSQL superuser (psql -U postgres -f create_database.sql).
-- Choose your own password; never commit it.
CREATE ROLE gola WITH LOGIN PASSWORD 'CHANGE_ME';
CREATE DATABASE gola_grocery OWNER gola ENCODING 'UTF8';
