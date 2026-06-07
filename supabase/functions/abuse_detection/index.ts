// abuse_detection — Supabase Edge Function
// Daily automated safety signal detection (spec §6.3, §8.4).
//
// Signal 1: 3+ reports within 30 days  → admin_flagged = true (priority review queue)
// Signal 2: show_up_rate < 40% with ≥3 judged runs → is_suspended = true (run creation blocked)
// Signal 3: pace deviation (3+ consecutive runs >2min/km outside zone) → handled in Task 6.6
//
// Deploy: supabase functions deploy abuse_detection
//
// Schedule (run at 02:00 UTC every day):
//   select cron.schedule('abuse-detection-daily', '0 2 * * *', $$
//     select net.http_post(
//       url := current_setting('app.supabase_url') || '/functions/v1/abuse_detection',
//       headers := '{"Authorization":"Bearer <service-role-key>","Content-Type":"application/json"}'::jsonb,
//       body := '{}'::jsonb
//     );
//   $$);
//
// Environment (auto-injected by Supabase):
//   SUPABASE_URL              — project REST URL
//   SUPABASE_SERVICE_ROLE_KEY — bypasses RLS for admin writes

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY  = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const REPORT_WINDOW_DAYS     = 30;
const REPORT_COUNT_THRESHOLD = 3;

Deno.serve(async (_req: Request): Promise<Response> => {
  try {
    const supabase = createClient(SUPABASE_URL, SERVICE_KEY);

    const results = {
      flagged_for_review: 0,
      suspended:          0,
      errors:             [] as string[],
    };

    // ── Signal 1: excessive reports → admin_flagged = true ────────────────

    const { data: flagCandidates, error: flagFetchError } = await supabase
      .rpc("fn_users_with_excessive_reports", {
        window_days: REPORT_WINDOW_DAYS,
        min_count:   REPORT_COUNT_THRESHOLD,
      });

    if (flagFetchError) {
      console.error("abuse_detection: fn_users_with_excessive_reports failed", flagFetchError);
      results.errors.push(`report_fetch: ${flagFetchError.message}`);
    } else {
      const idsToFlag = (flagCandidates as { user_id: string }[] | null)
        ?.map((r) => r.user_id) ?? [];

      if (idsToFlag.length > 0) {
        const { error: flagError } = await supabase
          .from("users")
          .update({ admin_flagged: true, updated_at: new Date().toISOString() })
          .in("id", idsToFlag);

        if (flagError) {
          console.error("abuse_detection: update admin_flagged failed", flagError);
          results.errors.push(`flag_update: ${flagError.message}`);
        } else {
          results.flagged_for_review = idsToFlag.length;
          console.log(
            `abuse_detection: flagged ${idsToFlag.length} users for priority admin review`,
          );
        }
      } else {
        console.log("abuse_detection: no new users to flag for review");
      }
    }

    // ── Signal 2: low show-up rate → is_suspended = true ─────────────────

    const { data: suspendCandidates, error: suspendFetchError } = await supabase
      .rpc("fn_users_with_low_showup");

    if (suspendFetchError) {
      console.error("abuse_detection: fn_users_with_low_showup failed", suspendFetchError);
      results.errors.push(`suspend_fetch: ${suspendFetchError.message}`);
    } else {
      const idsToSuspend = (suspendCandidates as { user_id: string; show_up_rate: number }[] | null)
        ?.map((r) => r.user_id) ?? [];

      if (idsToSuspend.length > 0) {
        const { error: suspendError } = await supabase
          .from("users")
          .update({ is_suspended: true, updated_at: new Date().toISOString() })
          .in("id", idsToSuspend);

        if (suspendError) {
          console.error("abuse_detection: update is_suspended failed", suspendError);
          results.errors.push(`suspend_update: ${suspendError.message}`);
        } else {
          results.suspended = idsToSuspend.length;
          console.log(
            `abuse_detection: suspended ${idsToSuspend.length} users (show-up rate < 40%)`,
          );
        }
      } else {
        console.log("abuse_detection: no new users to suspend");
      }
    }

    const status = results.errors.length > 0 ? 207 : 200;
    return new Response(JSON.stringify(results), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  } catch (err) {
    console.error("abuse_detection: unexpected error", err);
    return new Response(
      JSON.stringify({ error: String(err) }),
      { status: 500, headers: { "Content-Type": "application/json" } },
    );
  }
});
