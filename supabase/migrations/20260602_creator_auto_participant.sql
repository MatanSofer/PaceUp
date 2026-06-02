-- Auto-insert run creator as accepted participant so they can access chat
-- and appear in participant list without needing to "join" their own run.
CREATE OR REPLACE FUNCTION add_creator_as_participant()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO run_participants (run_id, user_id, status)
    VALUES (NEW.id, NEW.creator_id, 'accepted')
    ON CONFLICT (run_id, user_id) DO NOTHING;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_creator_as_participant ON runs;
CREATE TRIGGER trg_creator_as_participant
    AFTER INSERT ON runs
    FOR EACH ROW EXECUTE FUNCTION add_creator_as_participant();

-- Backfill: add creator rows for all existing runs
INSERT INTO run_participants (run_id, user_id, status)
SELECT id, creator_id, 'accepted'
FROM runs
ON CONFLICT (run_id, user_id) DO NOTHING;
