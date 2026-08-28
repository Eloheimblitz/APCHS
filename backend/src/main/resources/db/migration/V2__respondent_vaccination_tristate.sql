-- respondent_vaccination becomes a tri-state Yes/No/NA field (String), matching
-- child_vaccination, instead of a plain boolean. Converts existing data in place
-- rather than dropping the column, so no survey responses are lost.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'survey_records' AND column_name = 'respondent_vaccination' AND data_type = 'boolean'
    ) THEN
        ALTER TABLE survey_records
            ALTER COLUMN respondent_vaccination TYPE varchar(10)
            USING (CASE
                WHEN respondent_vaccination IS TRUE THEN 'YES'
                WHEN respondent_vaccination IS FALSE THEN 'NO'
                ELSE NULL
            END);
    END IF;
END $$;
