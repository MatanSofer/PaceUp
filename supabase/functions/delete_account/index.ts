// delete_account — Supabase Edge Function
// Permanently deletes the calling user's account and all associated data (spec §5.2, §6.4).
// Must be called with the user's active session JWT in the Authorization header.
// Uses the service role key to delete the auth.users record, which cascades FK deletes.
//
// Deploy: supabase functions deploy delete_account
//
// Environment (auto-injected by Supabase):
//   SUPABASE_URL              — project REST URL
//   SUPABASE_SERVICE_ROLE_KEY — admin access to delete auth user
//   SUPABASE_ANON_KEY         — not needed; JWT is verified via service role getUser()

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY  = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

Deno.serve(async (req: Request): Promise<Response> => {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return new Response(
      JSON.stringify({ error: "Missing or invalid Authorization header" }),
      { status: 401, headers: { "Content-Type": "application/json" } },
    );
  }

  const jwt = authHeader.replace("Bearer ", "");
  const adminClient = createClient(SUPABASE_URL, SERVICE_KEY, {
    auth: { persistSession: false },
  });

  // Verify the JWT and extract the user ID.
  const { data: { user }, error: userError } = await adminClient.auth.getUser(jwt);
  if (userError || !user) {
    console.error("delete_account: JWT verification failed", userError);
    return new Response(
      JSON.stringify({ error: "Unauthorized" }),
      { status: 401, headers: { "Content-Type": "application/json" } },
    );
  }

  const userId = user.id;
  console.log(`delete_account: deleting user ${userId}`);

  // Deleting from auth.users cascades to public tables that reference users(id).
  // All FK constraints with ON DELETE CASCADE will be handled automatically.
  // Tables without cascade are cleaned up below before deleting the auth user.

  // Clean up push token so notifications stop immediately.
  await adminClient.from("users").update({ push_token: null }).eq("id", userId);

  // Cancel any open runs created by this user so participants are not left waiting.
  await adminClient
    .from("runs")
    .update({ status: "cancelled" })
    .eq("creator_id", userId)
    .in("status", ["open", "full"]);

  // Delete the auth user — cascades will handle the rest.
  const { error: deleteError } = await adminClient.auth.admin.deleteUser(userId);
  if (deleteError) {
    console.error(`delete_account: auth.admin.deleteUser failed for ${userId}`, deleteError);
    return new Response(
      JSON.stringify({ error: deleteError.message }),
      { status: 500, headers: { "Content-Type": "application/json" } },
    );
  }

  console.log(`delete_account: successfully deleted user ${userId}`);
  return new Response(
    JSON.stringify({ deleted: true }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
});
