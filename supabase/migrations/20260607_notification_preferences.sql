-- Migration: notification preferences (Task 8.5)
-- Adds per-user notification preference columns to the users table.
-- All default true (notifications on by default per spec §5.2).
-- Checked by the notification_dispatcher before FCM delivery.

ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS notif_run_reminders      boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notif_join_requests       boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notif_rival_nudges        boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notif_rival_summary       boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notif_new_runs            boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notif_partner_ratings     boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notif_marketing           boolean NOT NULL DEFAULT true;

COMMENT ON COLUMN public.users.notif_run_reminders  IS '24h and 2h pre-run reminders.';
COMMENT ON COLUMN public.users.notif_join_requests  IS 'Creator notified when someone requests to join their run.';
COMMENT ON COLUMN public.users.notif_rival_nudges   IS 'Rival just ran notifications.';
COMMENT ON COLUMN public.users.notif_rival_summary  IS 'Sunday evening weekly rival recap.';
COMMENT ON COLUMN public.users.notif_new_runs       IS 'New matching run opened nearby.';
COMMENT ON COLUMN public.users.notif_partner_ratings IS 'Post-run rating prompts.';
COMMENT ON COLUMN public.users.notif_marketing      IS 'App updates and announcements.';
