-- Migration: reputation recalculation (spec §4.4, §8.4 reputation_recalculate)
-- Provides recalculate_user_reputation(uuid) SQL function and a trigger that
-- calls it automatically after run_participants.status transitions to a
-- terminal state (attended / no_show / late_cancel).
--
-- Tier rules (pace_accuracy deferred to Post-MVP per Task 6.3 note):
--   new_runner : total attended runs < 3
--   active     : total attended runs >= 3
--   trusted    : active AND show_up_rate > 85
--   pacer_eligible: Post-MVP

-- ─── Core calculation function ───────────────────────────────────────────

CREATE OR REPLACE FUNCTION recalculate_user_reputation(p_user_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_attended        integer;
  v_no_show         integer;
  v_late_cancel     integer;
  v_total_judged    integer;
  v_show_up_rate    float;
  v_unique_partners integer;
  v_tier            text;
BEGIN
  SELECT
    COUNT(*) FILTER (WHERE status = 'attended'),
    COUNT(*) FILTER (WHERE status = 'no_show'),
    COUNT(*) FILTER (WHERE status = 'late_cancel')
  INTO v_attended, v_no_show, v_late_cancel
  FROM public.run_participants
  WHERE user_id = p_user_id;

  v_total_judged := v_attended + v_no_show + v_late_cancel;

  v_show_up_rate := CASE
    WHEN v_total_judged = 0 THEN 100.0
    ELSE ROUND((v_attended::float / v_total_judged) * 100.0, 1)
  END;

  SELECT COUNT(DISTINCT rp2.user_id)
  INTO v_unique_partners
  FROM public.run_participants rp1
  JOIN public.run_participants rp2
    ON rp1.run_id  = rp2.run_id
   AND rp2.user_id != p_user_id
   AND rp2.status  = 'attended'
  WHERE rp1.user_id = p_user_id
    AND rp1.status  = 'attended';

  v_tier := CASE
    WHEN v_attended < 3      THEN 'new_runner'
    WHEN v_show_up_rate > 85 THEN 'trusted'
    ELSE                          'active'
  END;

  UPDATE public.users
  SET
    show_up_rate      = v_show_up_rate,
    total_paceup_runs = v_attended,
    unique_partners   = v_unique_partners,
    reputation_tier   = v_tier,
    updated_at        = now()
  WHERE id = p_user_id;
END;
$$;

-- ─── Trigger function ─────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION trigger_recalculate_reputation()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  IF NEW.status IN ('attended', 'no_show', 'late_cancel')
     AND (OLD.status IS NULL OR OLD.status NOT IN ('attended', 'no_show', 'late_cancel'))
  THEN
    PERFORM recalculate_user_reputation(NEW.user_id);
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_participant_status_terminal ON public.run_participants;

CREATE TRIGGER on_participant_status_terminal
  AFTER UPDATE ON public.run_participants
  FOR EACH ROW
  EXECUTE FUNCTION trigger_recalculate_reputation();
