// run_attendance_verify — Supabase Edge Function
// Scheduled via pg_cron every 30 minutes (see migration: attendance_verify_helpers).
// For each run that ended 10-90 minutes ago with unverified accepted participants:
//   1. Fetches each participant's Strava OAuth token.
//   2. Refreshes the token if it has expired.
//   3. Calls the Strava activities API to look for a matching run.
//   4. Match criteria (spec §7.1): activity is a running sport type,
//      started within ±30min of the run's scheduled_at,
//      and started within 500m of the run's meeting point (if GPS is available).
//   5. Sets run_participants.status to 'attended' or 'no_show'.
//      Participants without a Strava token are left as 'accepted' (cannot verify).
//
// Deploy: supabase functions deploy run_attendance_verify
//
// Required Supabase secrets (supabase secrets set):
//   STRAVA_CLIENT_ID      — Strava app client ID
//   STRAVA_CLIENT_SECRET  — Strava app client secret
//
// Auto-injected by Supabase:
//   SUPABASE_URL              — project REST URL
//   SUPABASE_SERVICE_ROLE_KEY — bypasses RLS for all DB reads/writes

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL   = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY    = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const STRAVA_CLIENT_ID     = Deno.env.get("STRAVA_CLIENT_ID") ?? "";
const STRAVA_CLIENT_SECRET = Deno.env.get("STRAVA_CLIENT_SECRET") ?? "";

const STRAVA_OAUTH_URL = "https://www.strava.com/oauth/token";
const STRAVA_API_URL   = "https://www.strava.com/api/v3";

const MATCH_RADIUS_M   = 500;   // spec §7.1: within 500m of meeting point
const MATCH_WINDOW_MIN = 30;    // spec §7.1: within ±30min of scheduled_at

// ─── Interfaces ────────────────────────────────────────────────────────────

interface RunToVerify {
  run_id: string;
  meeting_lat: number;
  meeting_lng: number;
  scheduled_at: string;
  duration_min: number | null;
}

interface Participant {
  id: string;
  user_id: string;
}

interface StoredStravaToken {
  user_id: string;
  access_token: string;
  refresh_token: string;
  expires_at: number; // Unix epoch seconds
}

interface StravaTokenResponse {
  access_token: string;
  refresh_token: string;
  expires_at: number;
}

/** Summary shape from GET /api/v3/athlete/activities. */
interface StravaActivity {
  id: number;
  sport_type: string;
  start_date: string;           // ISO 8601 UTC
  distance: number;             // meters
  moving_time: number;          // seconds
  start_latlng: [number, number] | null;
}

// ─── Helpers ───────────────────────────────────────────────────────────────

function haversineMeters(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6_371_000; // Earth radius in metres
  const toRad = (x: number) => (x * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

const RUNNING_SPORT_TYPES = new Set(["Run", "TrailRun", "VirtualRun", "Treadmill", "RaceRun"]);

/** Returns whether a Strava activity matches the run's time and location criteria. */
function isMatchingActivity(
  activity: StravaActivity,
  runLat: number,
  runLng: number,
  scheduledAtMs: number,
): boolean {
  if (!RUNNING_SPORT_TYPES.has(activity.sport_type)) return false;

  const activityStartMs = new Date(activity.start_date).getTime();
  const windowMs = MATCH_WINDOW_MIN * 60_000;
  const withinTimeWindow =
    activityStartMs >= scheduledAtMs - windowMs &&
    activityStartMs <= scheduledAtMs + windowMs;

  if (!withinTimeWindow) return false;

  // If GPS start is available, check distance; if not (private activity), accept by time alone.
  if (activity.start_latlng) {
    const [actLat, actLng] = activity.start_latlng;
    const distM = haversineMeters(runLat, runLng, actLat, actLng);
    return distM <= MATCH_RADIUS_M;
  }

  // start_latlng is null → GPS hidden by Strava privacy settings → accept on time match.
  return true;
}

/** Refreshes an expired Strava token. Returns updated token or null on failure. */
async function refreshStravaToken(
  stored: StoredStravaToken,
): Promise<StravaTokenResponse | null> {
  try {
    const res = await fetch(STRAVA_OAUTH_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        client_id: STRAVA_CLIENT_ID,
        client_secret: STRAVA_CLIENT_SECRET,
        grant_type: "refresh_token",
        refresh_token: stored.refresh_token,
      }),
    });
    if (!res.ok) return null;
    return (await res.json()) as StravaTokenResponse;
  } catch {
    return null;
  }
}

/** Fetches Strava activities in the given Unix second window. Returns [] on error. */
async function fetchStravaActivities(
  accessToken: string,
  afterEpoch: number,
  beforeEpoch: number,
): Promise<StravaActivity[]> {
  try {
    const url = new URL(`${STRAVA_API_URL}/athlete/activities`);
    url.searchParams.set("after", String(afterEpoch));
    url.searchParams.set("before", String(beforeEpoch));
    url.searchParams.set("per_page", "20");

    const res = await fetch(url.toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!res.ok) return [];
    return (await res.json()) as StravaActivity[];
  } catch {
    return [];
  }
}

// ─── Main handler ──────────────────────────────────────────────────────────

Deno.serve(async (_req: Request): Promise<Response> => {
  if (!STRAVA_CLIENT_ID || !STRAVA_CLIENT_SECRET) {
    console.error("run_attendance_verify: STRAVA_CLIENT_ID / STRAVA_CLIENT_SECRET not set");
    return new Response(JSON.stringify({ error: "missing Strava credentials" }), { status: 500 });
  }

  const supabase = createClient(SUPABASE_URL, SERVICE_KEY);

  // Step 1: find runs that ended recently with unverified accepted participants.
  const { data: runs, error: runsError } = await supabase
    .rpc("get_runs_for_attendance_verify") as { data: RunToVerify[] | null; error: unknown };

  if (runsError) {
    console.error("run_attendance_verify: get_runs_for_attendance_verify failed", runsError);
    return new Response(JSON.stringify({ error: String(runsError) }), { status: 500 });
  }

  if (!runs || runs.length === 0) {
    console.log("run_attendance_verify: no runs to verify");
    return new Response(JSON.stringify({ verified: 0 }), { status: 200 });
  }

  console.log(`run_attendance_verify: ${runs.length} run(s) to verify`);

  let totalAttended = 0;
  let totalNoShow   = 0;
  let totalSkipped  = 0; // no Strava token

  for (const run of runs) {
    const scheduledAtMs = new Date(run.scheduled_at).getTime();
    const durationMs    = (run.duration_min ?? 60) * 60_000;

    // Search window: 30min before scheduled start → end + 30min
    const afterEpoch  = Math.floor((scheduledAtMs - MATCH_WINDOW_MIN * 60_000) / 1000);
    const beforeEpoch = Math.floor((scheduledAtMs + durationMs + MATCH_WINDOW_MIN * 60_000) / 1000);

    // Step 2: get accepted participants for this run.
    const { data: participants, error: partError } = await supabase
      .from("run_participants")
      .select("id, user_id")
      .eq("run_id", run.run_id)
      .eq("status", "accepted") as { data: Participant[] | null; error: unknown };

    if (partError || !participants || participants.length === 0) continue;

    // Step 3: fetch tokens for all participants in one query.
    const userIds = participants.map((p) => p.user_id);
    const { data: tokens } = await supabase
      .from("user_strava_tokens")
      .select("user_id, access_token, refresh_token, expires_at")
      .in("user_id", userIds) as { data: StoredStravaToken[] | null; error: unknown };

    const tokenMap = new Map<string, StoredStravaToken>(
      (tokens ?? []).map((t) => [t.user_id, t]),
    );

    // Step 4: verify each participant.
    for (const participant of participants) {
      let stored = tokenMap.get(participant.user_id);

      if (!stored) {
        // No Strava token → cannot verify; leave as 'accepted'.
        totalSkipped++;
        continue;
      }

      // Refresh token if expired (add 60s buffer to avoid last-second expiry).
      const nowEpoch = Math.floor(Date.now() / 1000);
      if (stored.expires_at < nowEpoch + 60) {
        const refreshed = await refreshStravaToken(stored);
        if (!refreshed) {
          console.warn(`run_attendance_verify: token refresh failed for user ${participant.user_id}`);
          totalSkipped++;
          continue;
        }
        // Persist refreshed token.
        await supabase.from("user_strava_tokens").upsert({
          user_id:       participant.user_id,
          access_token:  refreshed.access_token,
          refresh_token: refreshed.refresh_token,
          expires_at:    refreshed.expires_at,
        });
        stored = { ...stored, ...refreshed };
      }

      // Fetch Strava activities in the window.
      const activities = await fetchStravaActivities(
        stored.access_token,
        afterEpoch,
        beforeEpoch,
      );

      // Find a matching activity.
      const match = activities.find((a) =>
        isMatchingActivity(a, run.meeting_lat, run.meeting_lng, scheduledAtMs),
      );

      if (match) {
        // Derive pace: moving_time (sec) / distance_km — spec §7.1: raw data not stored.
        const distanceKm     = match.distance / 1000;
        const actualAvgPace  = distanceKm > 0
          ? Math.round(match.moving_time / distanceKm)
          : null;

        await supabase
          .from("run_participants")
          .update({
            status:            "attended",
            strava_activity_id: String(match.id),
            actual_avg_pace:   actualAvgPace,
            attended_at:       new Date().toISOString(),
          })
          .eq("id", participant.id);

        totalAttended++;
        console.log(`run_attendance_verify: ATTENDED user=${participant.user_id} run=${run.run_id} activityId=${match.id}`);
      } else {
        await supabase
          .from("run_participants")
          .update({ status: "no_show" })
          .eq("id", participant.id);

        totalNoShow++;
        console.log(`run_attendance_verify: NO_SHOW user=${participant.user_id} run=${run.run_id}`);
      }
    }
  }

  console.log(
    `run_attendance_verify: done — attended=${totalAttended} no_show=${totalNoShow} skipped=${totalSkipped}`,
  );
  return new Response(
    JSON.stringify({ attended: totalAttended, no_show: totalNoShow, skipped: totalSkipped }),
    { status: 200 },
  );
});
