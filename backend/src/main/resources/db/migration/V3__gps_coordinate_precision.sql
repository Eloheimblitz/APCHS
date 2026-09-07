-- latitude/longitude had no explicit precision, so Hibernate defaulted to
-- numeric(38,2), silently rounding GPS coordinates to ~1km accuracy on every
-- save even though the app captures 7 decimal places. Widen to numeric(12,7)
-- so new submissions keep meter-level precision. Already-stored coordinates
-- were already rounded on write and can't be recovered by this migration.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'survey_records') THEN
        ALTER TABLE survey_records ALTER COLUMN latitude TYPE numeric(12,7);
        ALTER TABLE survey_records ALTER COLUMN longitude TYPE numeric(12,7);
    END IF;
END $$;
