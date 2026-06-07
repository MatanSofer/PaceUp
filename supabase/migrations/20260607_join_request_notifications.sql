-- Migration: join request notifications (Task 8.4)
-- Fires when a user requests to join a run (run_participants INSERT with status = 'pending').
-- Notifies the run creator via the notifications table.
-- The notification_dispatcher Edge Function delivers via FCM.
-- data.run_id enables the creator to tap and open participant management.

CREATE OR REPLACE FUNCTION notify_creator_join_request()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_creator_id  uuid;
  v_run_title   text;
  v_requester   text;
BEGIN
  -- Only fire on new pending requests (not when creator is auto-added)
  IF NEW.status <> 'pending' THEN
    RETURN NEW;
  END IF;

  -- Fetch run creator and title
  SELECT creator_id, title
    INTO v_creator_id, v_run_title
    FROM public.runs
   WHERE id = NEW.run_id;

  -- Skip if the requester IS the creator (shouldn't happen, but guard anyway)
  IF v_creator_id IS NULL OR v_creator_id = NEW.user_id THEN
    RETURN NEW;
  END IF;

  -- Fetch the requester's display name
  SELECT display_name INTO v_requester
    FROM public.users
   WHERE id = NEW.user_id;

  -- Insert the notification (one per requester per run, idempotent)
  INSERT INTO public.notifications (user_id, type, title, body, data, sent, scheduled_at)
  SELECT
    v_creator_id,
    'join_request',
    'New join request',
    COALESCE(v_requester, 'Someone') || ' wants to join ' || COALESCE(v_run_title, 'your run') || '.',
    json_build_object(
      'type',           'join_request',
      'run_id',         NEW.run_id::text,
      'requester_id',   NEW.user_id::text
    )::jsonb,
    false,
    now()
  WHERE NOT EXISTS (
    SELECT 1
      FROM public.notifications n
     WHERE n.user_id              = v_creator_id
       AND n.type                 = 'join_request'
       AND n.data ->> 'run_id'    = NEW.run_id::text
       AND n.data ->> 'requester_id' = NEW.user_id::text
  );

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_join_request_notify_creator ON public.run_participants;

CREATE TRIGGER on_join_request_notify_creator
  AFTER INSERT ON public.run_participants
  FOR EACH ROW
  EXECUTE FUNCTION notify_creator_join_request();
