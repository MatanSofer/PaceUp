// reputation_recalculate — Supabase Edge Function
// Recalculates show_up_rate, total_paceup_runs, unique_partners, and
// reputation_tier for a given user (spec §4.4, §8.4).
//
// In normal operation this is handled automatically by the Postgres trigger
// on_participant_status_terminal (see migration: reputation_recalculate).
// This Edge Function exists for:
//   - Manual admin recalculation (e.g., after data corrections)
//   - Batch recalculation of all users (pass { "all": true })
//
// Tier logic (pace_accuracy is Post-MVP — not implemented here):
//   new_runner : attended runs < 3
//   active     : attended runs >= 3
//   trusted    : active AND show_up_rate > 85%
//   pacer_eligible : Post-MVP
//
// Deploy: supabase functions deploy reputation_recalculate
//
// Body (JSON):
//   { "user_id": "<uuid>" }   — recalculate one user
//   { "all": true }           — recalculate all non-banned users (admin only)
//
// Environment (auto-injected by Supabase):
//   SUPABASE_URL              — project REST URL
//   SUPABASE_SERVICE_ROLE_KEY — bypasses RLS

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY  = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

Deno.serve(async (req: Request): Promise<Response> => {
  let body: { user_id?: string; all?: boolean } = {};
  try {
    body = await req.json();
  } catch {
    // Empty body is allowed (treat as no-op).
  }

  const supabase = createClient(SUPABASE_URL, SERVICE_KEY);

  if (body.all === true) {
    // Batch: recalculate every non-banned user.
    const { data: users, error: usersError } = await supabase
      .from("users")
      .select("id")
      .eq("is_banned", false);

    if (usersError) {
      console.error("reputation_recalculate: fetch users failed", usersError);
      return new Response(JSON.stringify({ error: usersError.message }), { status: 500 });
    }

    let recalculated = 0;
    for (const user of (users ?? [])) {
      const { error } = await supabase.rpc("recalculate_user_reputation", { p_user_id: user.id });
      if (error) {
        console.error(`reputation_recalculate: failed for user ${user.id}`, error);
      } else {
        recalculated++;
      }
    }

    console.log(`reputation_recalculate: batch done — ${recalculated} users updated`);
    return new Response(JSON.stringify({ recalculated }), { status: 200 });
  }

  if (!body.user_id) {
    return new Response(
      JSON.stringify({ error: "provide user_id or { all: true }" }),
      { status: 400 },
    );
  }

  const { error } = await supabase.rpc("recalculate_user_reputation", {
    p_user_id: body.user_id,
  });

  if (error) {
    console.error(`reputation_recalculate: failed for user ${body.user_id}`, error);
    return new Response(JSON.stringify({ error: error.message }), { status: 500 });
  }

  console.log(`reputation_recalculate: success user=${body.user_id}`);
  return new Response(JSON.stringify({ recalculated: 1 }), { status: 200 });
});
