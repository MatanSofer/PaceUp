-- Migration: rival nudge notifications (Task 8.3)
--
-- Part 1: "your rival just ran" trigger.
--   Fires after run_participants.status transitions to 'attended'.
--   For each active rival of the attendee, inserts a rival_nudge notification row.
--   The notification_dispatcher Edge Function delivers these via FCM.
--
-- Part 2: Sunday 19:00 UTC weekly summary.
--   pg_cron job inserts a rival_weekly_summary notification for every user
--   who has at least one active rival (both sides of the pair notified).
--   NOT EXISTS guard prevents duplicates within the same 7-day window.

-- ─── Part 1: rival ran trigger ───────────────────────────────────────────────

CREATE OR REPLACE FUNCTION notify_rival_ran()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_runner_name text;
  v_rival_user  uuid;
BEGIN
  -- Only fire when status transitions to 'attended'
  IF NEW.status <> 'attended' THEN
    RETURN NEW;
  END IF;
  IF TG_OP = 'UPDATE' AND OLD.status = 'attended' THEN
    RETURN NEW;  -- already marked attended, avoid re-firing
  END IF;

  -- Fetch the runner's display name for the notification body
  SELECT display_name INTO v_runner_name
    FROM public.users
   WHERE id = NEW.user_id;

  -- Notify each active rival of this user
  FOR v_rival_user IN
    SELECT CASE
             WHEN r.user_a_id = NEW.user_id THEN r.user_b_id
             ELSE r.user_a_id
           END
      FROM public.rivals r
     WHERE r.status = 'active'
       AND (r.user_a_id = NEW.user_id OR r.user_b_id = NEW.user_id)
  LOOP
    -- One notification per rival per run (no duplicates)
    INSERT INTO public.notifications (user_id, type, title, body, data, sent, scheduled_at)
    SELECT
      v_rival_user,
      'rival_nudge',
      'Your rival just ran!',
      COALESCE(v_runner_name, 'Your rival') || ' completed a run. Don''t fall behind!',
      json_build_object(
        'type',          'rival_nudge',
        'rival_user_id', NEW.user_id::text,
        'run_id',        NEW.run_id::text
      )::jsonb,
      false,
      now()
    WHERE NOT EXISTS (
      SELECT 1
        FROM public.notifications n
       WHERE n.user_id      = v_rival_user
         AND n.type         = 'rival_nudge'
         AND n.data ->> 'run_id' = NEW.run_id::text
    );
  END LOOP;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_participant_attended_notify_rivals ON public.run_participants;

CREATE TRIGGER on_participant_attended_notify_rivals
  AFTER UPDATE OF status ON public.run_participants
  FOR EACH ROW
  EXECUTE FUNCTION notify_rival_ran();

-- ─── Part 2: Sunday evening weekly summary (pg_cron) ─────────────────────────

-- Helper function called by pg_cron to insert summary notifications.
CREATE OR REPLACE FUNCTION insert_rival_weekly_summaries()
RETURNS void
LANGUAGE sql
SECURITY DEFINER
AS $$
  WITH both_sides AS (
    SELECT user_a_id AS user_id FROM public.rivals WHERE status = 'active'
    UNION ALL
    SELECT user_b_id AS user_id FROM public.rivals WHERE status = 'active'
  )
  INSERT INTO public.notifications (user_id, type, title, body, data, sent, scheduled_at)
  SELECT DISTINCT
    bs.user_id,
    'rival_weekly_summary',
    'Weekly rival recap is in!',
    'Check your rival dashboard to see this week''s results.',
    '{"type": "rival_weekly_summary"}'::jsonb,
    false,
    now()
  FROM both_sides bs
  WHERE NOT EXISTS (
    SELECT 1
      FROM public.notifications n
     WHERE n.user_id       = bs.user_id
       AND n.type          = 'rival_weekly_summary'
       AND n.scheduled_at  > now() - INTERVAL '7 days'
  );
$$;

-- Schedule Sunday 19:00 UTC weekly summary (pg_cron required — Supabase Pro+).
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
    PERFORM cron.schedule(
      'rival-weekly-summary',
      '0 19 * * 0',  -- every Sunday at 19:00 UTC
      $cron$ SELECT insert_rival_weekly_summaries(); $cron$
    );
  END IF;
END;
$$;
