// run_cancellation_notify — Supabase Edge Function
// Triggered by a Postgres trigger (pg_net) on UPDATE to public.runs when
// status changes to 'cancelled'. Sends a push notification to every accepted
// participant so they know the run has been called off.
//
// Deploy: supabase functions deploy run_cancellation_notify
//
// Environment (auto-injected by Supabase):
//   SUPABASE_URL              — project REST URL
//   SUPABASE_SERVICE_ROLE_KEY — bypasses RLS to read participants and insert notifications
//
// Trigger payload (POST body from pg_net):
//   { "run_id": "<uuid>", "title": "<string>", "cancellation_reason": "<string | null>" }

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY  = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

interface TriggerPayload {
  run_id: string;
  title: string | null;
  cancellation_reason: string | null;
}

interface ParticipantRow {
  user_id: string;
}

Deno.serve(async (req: Request): Promise<Response> => {
  try {
    const payload: TriggerPayload = await req.json();
    const { run_id, title, cancellation_reason } = payload;

    if (!run_id) {
      return new Response(JSON.stringify({ error: "run_id is required" }), { status: 400 });
    }

    const supabase = createClient(SUPABASE_URL, SERVICE_KEY);

    // Fetch all accepted participants for this run
    const { data: participants, error: fetchError } = await supabase
      .from("run_participants")
      .select("user_id")
      .eq("run_id", run_id)
      .eq("status", "accepted");

    if (fetchError) {
      console.error("run_cancellation_notify: fetch participants error", fetchError);
      return new Response(JSON.stringify({ error: fetchError.message }), { status: 500 });
    }

    if (!participants || participants.length === 0) {
      console.log(`run_cancellation_notify: no accepted participants for run ${run_id}`);
      return new Response(JSON.stringify({ notified: 0 }), { status: 200 });
    }

    const runLabel = title ?? "A run you joined";
    const body = cancellation_reason
      ? `Cancelled: ${cancellation_reason}`
      : "The organiser has cancelled this run.";

    // Insert a notification row per participant — notification_dispatcher sends the actual push
    const notifications = (participants as ParticipantRow[]).map((p) => ({
      user_id: p.user_id,
      type: "run_cancelled",
      title: `${runLabel} has been cancelled`,
      body,
      data: JSON.stringify({ run_id }),
      sent: false,
    }));

    const { error: insertError } = await supabase
      .from("notifications")
      .insert(notifications);

    if (insertError) {
      console.error("run_cancellation_notify: insert notifications error", insertError);
      return new Response(JSON.stringify({ error: insertError.message }), { status: 500 });
    }

    console.log(`run_cancellation_notify: notified ${notifications.length} participants for run ${run_id}`);
    return new Response(JSON.stringify({ notified: notifications.length }), { status: 200 });
  } catch (err) {
    console.error("run_cancellation_notify: unexpected error", err);
    return new Response(JSON.stringify({ error: String(err) }), { status: 500 });
  }
});
