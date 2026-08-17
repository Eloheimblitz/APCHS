-- Guarantees survey_id/household_id uniqueness at the database level, independent of
-- Hibernate's ddl-auto=update (which is not guaranteed to retroactively add constraints
-- to an already-existing column). Guarded so it is a safe no-op on a brand-new database
-- where survey_records does not exist yet; ddl-auto creates the constraint there instead.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'survey_records') THEN
        CREATE UNIQUE INDEX IF NOT EXISTS ux_survey_records_survey_id ON survey_records (survey_id);
        CREATE UNIQUE INDEX IF NOT EXISTS ux_survey_records_household_id ON survey_records (household_id);
    END IF;
END $$;
