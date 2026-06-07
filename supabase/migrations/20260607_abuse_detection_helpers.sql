-- Migration: abuse detection helpers (spec §6.3, §8.4 abuse_detection)
-- Adds admin_flagged column to users and SQL helper functions used by the
-- abuse_detection Edge Function (run daily).
--
-- Two automated signals:
--   1. 3+ reports in 30 days → admin_flagged = true (priority review queue)
--   2. show_up_rate < 40% with ≥3 judged runs → is_suspended = true (run creation blocked)
-- Signal 3 (pace deviation) is handled in Task 6.6.

-- ─── Add admin_flagged column ─────────────────────────────────────────────

ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS admin_flagged BOOLEAN NOT NULL DEFAULT false;

-- ─── Update run creation policy to also block suspended users ─────────────
-- The existing policy in access_tier_enforcement.sql only checks reputation_tier.
-- Suspended users must also be blocked from creating runs.

DROP POLICY IF EXISTS "active users can create runs" ON public.runs;

CREATE POLICY "active users can create runs" ON public.runs
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.users
      WHERE id = auth.uid()
        AND reputation_tier IN ('active', 'trusted', 'pacer_eligible')
        AND is_suspended = false
    )
  );

-- ─── Helper: users with 3+ reports in a rolling window ───────────────────
-- Returns users not yet admin_flagged who now have >= min_count reports
-- within the last window_days days.

CREATE OR REPLACE FUNCTION fn_users_with_excessive_reports(
  window_days int,
  min_count   int
)
RETURNS TABLE(user_id uuid, report_count bigint)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT r.target_id, COUNT(*) AS report_count
  FROM   public.reports r
  JOIN   public.users   u ON u.id = r.target_id
  WHERE  r.target_type      = 'user'
    AND  r.target_id        IS NOT NULL
    AND  r.created_at        > now() - (window_days || ' days')::interval
    AND  u.admin_flagged      = false
  GROUP  BY r.target_id
  HAVING COUNT(*) >= min_count;
$$;

-- ─── Helper: users with low show-up rate and enough history ──────────────
-- A user is eligible for suspension when:
--   • not already banned or suspended
--   • show_up_rate < 40%
--   • has at least 3 judged runs (attended + no_show + late_cancel)
--     — this prevents suspending a user who missed a single run.

CREATE OR REPLACE FUNCTION fn_users_with_low_showup()
RETURNS TABLE(user_id uuid, show_up_rate float)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT u.id, u.show_up_rate
  FROM   public.users u
  WHERE  u.is_banned    = false
    AND  u.is_suspended = false
    AND  u.show_up_rate < 40.0
    AND  (
      SELECT COUNT(*)
      FROM   public.run_participants rp
      WHERE  rp.user_id = u.id
        AND  rp.status IN ('attended', 'no_show', 'late_cancel')
    ) >= 3;
$$;
