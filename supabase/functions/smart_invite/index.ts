// smart_invite — Supabase Edge Function
// Called by a Postgres trigger (pg_net) immediately after every INSERT on public.runs.
// Finds nearby runners whose avg pace matches the new run's pace range, then creates
// notification records that notification_dispatcher picks up and sends via FCM/APNs.
//
// Deploy: supabase functions deploy smart_invite
//
// Environment (auto-injected by Supabase):
//   SUPABASE_URL              — project REST URL
//   SUPABASE_SERVICE_ROLE_KEY — bypasses RLS for user queries and notification inserts

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY  = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const DISCOVERY_RADIUS_KM = 50; // matches the client-side discovery radius in SupabaseRunRepository
const MAX_INVITES = 200;         // cap to prevent notification spam

interface RunRow {
  id: string;
  creator_id: string;
  city: string;
  meeting_lat: number;
  meeting_lng: number;
  pace_min_sec: number;
  pace_max_sec: number;
  mode: string;
  distance_km: number | null;
  duration_min: number | null;
  scheduled_at: string;
  verified_only: boolean;
  status?: string;
}

interface UserCandidate {
  id: string;
  lat: number;
  lng: number;
}

function haversineKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371;
  const toRad = (x: number) => (x * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function buildNotificationBody(run: RunRow): string {
  const date = new Date(run.scheduled_at);
  const dateStr = date.toLocaleDateString("en-US", { month: "short", day: "numeric" });
  const timeStr = date.toLocaleTimeString("en-US", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
  const modeLabel = run.mode.replace(/_/g, " ");
  const distPart = run.distance_km
    ? `${run.distance_km}km`
    : run.duration_min
    ? `${run.duration_min}min`
    : "";
  return `${modeLabel} run${distPart ? " — " + distPart : ""} on ${dateStr} at ${timeStr} in ${run.city}`;
}

Deno.serve(async (req) => {
  // pg_net sends the raw NEW row as the JSON body; Supabase DB Webhooks wrap it in { record: ... }
  let run: RunRow;
  try {
    const payload = await req.json();
    run = payload.record ?? payload;
  } catch {
    return new Response(JSON.stringify({ error: "invalid payload" }), { status: 400 });
  }

  if (!run?.id || !run?.creator_id) {
    return new Response(JSON.stringify({ error: "missing required run fields" }), { status: 400 });
  }

  // Skip cancelled or non-open runs (defensive check — trigger fires on INSERT so status is always "open")
  if (run.status && run.status !== "open") {
    return new Response(JSON.stringify({ skipped: "run not open" }), { status: 200 });
  }

  const supabase = createClient(SUPABASE_URL, SERVICE_KEY);

  // Build the user query: pace match + active account + has location data
  let query = supabase
    .from("users")
    .select("id, lat, lng")
    .eq("is_banned", false)
    .eq("is_suspended", false)
    .neq("id", run.creator_id)
    .not("avg_pace_seconds", "is", null)
    .not("lat", "is", null)
    .not("lng", "is", null)
    .gte("avg_pace_seconds", run.pace_min_sec)
    .lte("avg_pace_seconds", run.pace_max_sec)
    .limit(500);

  // Only add the is_verified filter when the run requires it
  if (run.verified_only) {
    query = query.eq("is_verified", true);
  }

  const { data: candidates, error: usersError } = await query;

  if (usersError) {
    console.error("smart_invite: users query failed:", usersError.message);
    return new Response(JSON.stringify({ error: usersError.message }), { status: 500 });
  }

  // Filter by distance in JS — avoids a PostGIS dependency
  const withinRadius = ((candidates ?? []) as UserCandidate[])
    .filter((u) => haversineKm(run.meeting_lat, run.meeting_lng, u.lat, u.lng) <= DISCOVERY_RADIUS_KM)
    .slice(0, MAX_INVITES);

  if (withinRadius.length === 0) {
    console.log(`smart_invite: no matching runners for run ${run.id}`);
    return new Response(JSON.stringify({ invited: 0 }), { status: 200 });
  }

  // Exclude users who already received a smart_invite for this run (idempotency)
  const { data: existing } = await supabase
    .from("notifications")
    .select("user_id")
    .eq("type", "smart_invite")
    .contains("data", { run_id: run.id });

  const alreadyInvited = new Set<string>((existing ?? []).map((n: any) => n.user_id as string));
  const toInvite = withinRadius.filter((u) => !alreadyInvited.has(u.id));

  if (toInvite.length === 0) {
    return new Response(JSON.stringify({ invited: 0, reason: "already invited" }), { status: 200 });
  }

  const body = buildNotificationBody(run);
  const now = new Date().toISOString();

  const rows = toInvite.map((u) => ({
    user_id: u.id,
    type: "smart_invite",
    title: "New run near you",
    body,
    data: { run_id: run.id, type: "smart_invite" },
    scheduled_at: now,
    sent: false,
  }));

  const { error: insertError } = await supabase.from("notifications").insert(rows);

  if (insertError) {
    console.error("smart_invite: notification insert failed:", insertError.message);
    return new Response(JSON.stringify({ error: insertError.message }), { status: 500 });
  }

  console.log(`smart_invite: run ${run.id} — invited ${toInvite.length} runners`);
  return new Response(JSON.stringify({ invited: toInvite.length }), { status: 200 });
});
