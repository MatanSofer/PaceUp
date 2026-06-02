-- Migration: attendance verification SQL helpers
-- Provides get_runs_for_attendance_verify() used by the run_attendance_verify Edge Function (spec §8.4).
-- Also schedules the Edge Function via pg_cron every 30 minutes (requires Supabase Pro+).

-- Returns runs that ended 10-90 minutes ago with unverified accepted participants.
CREATE OR REPLACE FUNCTION get_runs_for_attendance_verify()
RETURNS TABLE(
  run_id       uuid,
  meeting_lat  real,
  meeting_lng  real,
  scheduled_at timestamptz,
  duration_min integer
)
LANGUAGE sql
SECURITY DEFINER
AS $$
  SELECT
    r.id          AS run_id,
    r.meeting_lat,
    r.meeting_lng,
    r.scheduled_at,
    r.duration_min
  FROM public.runs r
  WHERE
    r.status NOT IN ('cancelled')
    -- Run ended between 10 and 90 minutes ago
    AND (r.scheduled_at + (COALESCE(r.duration_min, 60) * INTERVAL '1 minute'))
        BETWEEN now() - INTERVAL '90 minutes' AND now() - INTERVAL '10 minutes'
    -- Has at least one accepted participant not yet verified
    AND EXISTS (
      SELECT 1
      FROM public.run_participants rp
      WHERE rp.run_id = r.id
        AND rp.status = 'accepted'
    );
$$;

-- Schedule run_attendance_verify to fire every 30 minutes (pg_cron + pg_net).
-- Only installed when pg_cron extension is present (Supabase Pro+).
-- To install manually: copy the cron.schedule(...) call below and run it in the SQL editor.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
    PERFORM cron.schedule(
      'run-attendance-verify',
      '*/30 * * * *',
      $cron$
        SELECT net.http_post(
          url     := current_setting('app.supabase_url') || '/functions/v1/run_attendance_verify',
          headers := json_build_object(
            'Authorization', 'Bearer ' || current_setting('app.service_role_key'),
            'Content-Type',  'application/json'
          )::jsonb,
          body    := '{}'::jsonb
        );
      $cron$
    );
  END IF;
END;
$$;
