// chat_cleanup — Supabase Edge Function
// Scheduled daily via Supabase cron (pg_cron).
// Deletes run_chat_messages for runs that ended more than 48 hours ago (spec §4.6, §8.4).
// "Run ended" is defined as: scheduled_at + (duration_min || 60) minutes < now - 48h.
//
// Deploy: supabase functions deploy chat_cleanup
// Schedule: select cron.schedule('chat-cleanup', '0 3 * * *', $$
//   select net.http_post(
//     url := current_setting('app.supabase_url') || '/functions/v1/chat_cleanup',
//     headers := '{"Authorization":"Bearer <service-role-key>","Content-Type":"application/json"}'::jsonb,
//     body := '{}'::jsonb
//   );
// $$);
//
// Environment (auto-injected by Supabase):
//   SUPABASE_URL              — project REST URL
//   SUPABASE_SERVICE_ROLE_KEY — bypasses RLS for deletion

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY  = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const ARCHIVE_HOURS = 48;
const DEFAULT_RUN_DURATION_MIN = 60;

Deno.serve(async (_req: Request): Promise<Response> => {
  try {
    const supabase = createClient(SUPABASE_URL, SERVICE_KEY);

    // Find runs that ended more than 48h ago.
    // End time = scheduled_at + duration_min (or 60min default).
    // We do the arithmetic in SQL via a raw RPC call (simpler than computing in JS).
    const { data: staleRunIds, error: fetchError } = await supabase
      .rpc("get_stale_chat_run_ids", { archive_hours: ARCHIVE_HOURS });

    if (fetchError) {
      console.error("chat_cleanup: fetch stale runs error", fetchError);
      return new Response(JSON.stringify({ error: fetchError.message }), { status: 500 });
    }

    const ids = (staleRunIds as { id: string }[] | null)?.map((r) => r.id) ?? [];

    if (ids.length === 0) {
      console.log("chat_cleanup: no stale chats found");
      return new Response(JSON.stringify({ deleted: 0 }), { status: 200 });
    }

    const { error: deleteError, count } = await supabase
      .from("run_chat_messages")
      .delete({ count: "exact" })
      .in("run_id", ids);

    if (deleteError) {
      console.error("chat_cleanup: delete error", deleteError);
      return new Response(JSON.stringify({ error: deleteError.message }), { status: 500 });
    }

    console.log(`chat_cleanup: deleted ${count} messages from ${ids.length} stale runs`);
    return new Response(JSON.stringify({ deleted: count, runs: ids.length }), { status: 200 });
  } catch (err) {
    console.error("chat_cleanup: unexpected error", err);
    return new Response(JSON.stringify({ error: String(err) }), { status: 500 });
  }
});
