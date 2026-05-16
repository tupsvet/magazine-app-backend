-- Adds rejection_reason; renames legacy reject_reason from V5 if present.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'magazines'
          AND column_name = 'reject_reason'
    ) THEN
        ALTER TABLE magazines RENAME COLUMN reject_reason TO rejection_reason;
    ELSIF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'magazines'
          AND column_name = 'rejection_reason'
    ) THEN
        ALTER TABLE magazines ADD COLUMN rejection_reason TEXT;
    END IF;
END $$;
