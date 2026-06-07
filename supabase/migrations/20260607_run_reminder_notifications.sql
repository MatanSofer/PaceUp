-- Migration: run reminder notifications trigger (Task 8.2)
-- Fires after a run_participants row is inserted or updated with status = 'accepted'.
-- Inserts two rows into the notifications table:
--   • 24h before run start
--   • 2h before run start
-- The notification_dispatcher Edge Function picks them up and sends via FCM.
-- Duplicate reminders are prevented with a WHERE NOT EXISTS guard.

CREATE OR REPLACE FUNCTION schedule_run_reminders()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_run_scheduled_at  timestamptz;
  v_run_title         text;
  v_remind_24h        timestamptz;
  v_remind_2h         timestamptz;
  v_data              jsonb;
BEGIN
  -- Only act when status transitions to 'accepted'
  IF NEW.status <> 'accepted' THEN
    RETURN NEW;
  END IF;
  IF TG_OP = 'UPDATE' AND OLD.status = 'accepted' THEN
    RETURN NEW;  -- already accepted, no duplicate scheduling
  END IF;

  -- Fetch run details
  SELECT scheduled_at, title
    INTO v_run_scheduled_at, v_run_title
    FROM public.runs
   WHERE id = NEW.run_id;

  IF v_run_scheduled_at IS NULL THEN
    RETURN NEW;
  END IF;

  v_remind_24h := v_run_scheduled_at - INTERVAL '24 hours';
  v_remind_2h  := v_run_scheduled_at - INTERVAL '2 hours';
  v_data       := json_build_object('run_id', NEW.run_id::text, 'type', 'run_reminder')::jsonb;

  -- 24h reminder (only if the reminder time is still in the future)
  IF v_remind_24h > now() THEN
    INSERT INTO public.notifications (user_id, type, title, body, data, sent, scheduled_at)
    SELECT
      NEW.user_id,
      'run_reminder',
      'Run tomorrow: ' || COALESCE(v_run_title, 'your run'),
      'You have a run scheduled in 24 hours. Get ready!',
      v_data,
      false,
      v_remind_24h
    WHERE NOT EXISTS (
      SELECT 1
        FROM public.notifications n
       WHERE n.user_id     = NEW.user_id
         AND n.type        = 'run_reminder'
         AND n.scheduled_at = v_remind_24h
         AND n.data        ->> 'run_id' = NEW.run_id::text
    );
  END IF;

  -- 2h reminder (only if the reminder time is still in the future)
  IF v_remind_2h > now() THEN
    INSERT INTO public.notifications (user_id, type, title, body, data, sent, scheduled_at)
    SELECT
      NEW.user_id,
      'run_reminder',
      'Run in 2 hours: ' || COALESCE(v_run_title, 'your run'),
      'Your run starts in 2 hours. Time to warm up!',
      v_data,
      false,
      v_remind_2h
    WHERE NOT EXISTS (
      SELECT 1
        FROM public.notifications n
       WHERE n.user_id     = NEW.user_id
         AND n.type        = 'run_reminder'
         AND n.scheduled_at = v_remind_2h
         AND n.data        ->> 'run_id' = NEW.run_id::text
    );
  END IF;

  RETURN NEW;
END;
$$;

-- Drop existing trigger to allow re-running
DROP TRIGGER IF EXISTS on_participant_accepted_schedule_reminders ON public.run_participants;

CREATE TRIGGER on_participant_accepted_schedule_reminders
  AFTER INSERT OR UPDATE OF status ON public.run_participants
  FOR EACH ROW
  EXECUTE FUNCTION schedule_run_reminders();
