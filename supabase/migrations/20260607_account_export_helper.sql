-- Migration: user data export helper (spec §6.4, GDPR Article 20)
-- fn_export_user_data() is called by the client via RPC to generate a full
-- JSON export of the calling user's data.

CREATE OR REPLACE FUNCTION fn_export_user_data()
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid    uuid := auth.uid();
  v_result jsonb;
BEGIN
  IF v_uid IS NULL THEN
    RAISE EXCEPTION 'Not authenticated';
  END IF;

  SELECT jsonb_build_object(
    'exported_at',     to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
    'profile',         (
      SELECT to_jsonb(u) - 'encrypted_password' - 'confirmation_token'
             - 'recovery_token' - 'email_change_token_new' - 'email_change_token_current'
             - 'reauthentication_token'
      FROM   public.users u
      WHERE  u.id = v_uid
    ),
    'runs_created',    (
      SELECT coalesce(jsonb_agg(to_jsonb(r)), '[]'::jsonb)
      FROM   public.runs r
      WHERE  r.creator_id = v_uid
    ),
    'run_participations', (
      SELECT coalesce(jsonb_agg(to_jsonb(rp)), '[]'::jsonb)
      FROM   public.run_participants rp
      WHERE  rp.user_id = v_uid
    ),
    'partner_ratings_given', (
      SELECT coalesce(jsonb_agg(to_jsonb(pr)), '[]'::jsonb)
      FROM   public.partner_ratings pr
      WHERE  pr.rater_id = v_uid
    ),
    'partner_ratings_received', (
      SELECT coalesce(jsonb_agg(to_jsonb(pr)), '[]'::jsonb)
      FROM   public.partner_ratings pr
      WHERE  pr.rated_user_id = v_uid
    ),
    'reports_filed',   (
      SELECT coalesce(jsonb_agg(to_jsonb(r)), '[]'::jsonb)
      FROM   public.reports r
      WHERE  r.reporter_id = v_uid
    ),
    'blocks',          (
      SELECT coalesce(jsonb_agg(to_jsonb(b)), '[]'::jsonb)
      FROM   public.user_blocks b
      WHERE  b.blocker_id = v_uid
    )
  ) INTO v_result;

  RETURN v_result;
END;
$$;
