-- Supply.reference_id points at a Location or a DisasterVictim, depending on reference_type.
-- V1 gave it a foreign key to both tables, so every row needed a matching id in each.
-- Dropping the two keys only relaxes the schema; no existing row can fail.
-- Known limit: the database no longer checks that the referenced row exists.
ALTER TABLE Supply DROP CONSTRAINT IF EXISTS supply_reference_id_fkey;
ALTER TABLE Supply DROP CONSTRAINT IF EXISTS supply_reference_id_fkey1;
