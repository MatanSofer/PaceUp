// weekly_pace_zone_update — Supabase Edge Function
// Scheduled via pg_cron every Monday at 03:00 UTC (see migration: weekly_pace_zone_update).
// For every Strava-connected user:
//   1. Fetches the last 90 days of Strava activities (runs > 3 km only — spec §4.1).
//   2. Calculates time-weighted average pace and assigns pace zone A–E.
//   3. Updates users.pace_zone, avg_pace_seconds, weekly_mileage_avg.
//   4. Checks the last 3 attended runs for systematic pace deviation.
//      If actual_avg_pace deviates > 2 min/km from the pre-update stated pace
//      on 3+ consecutive attended runs → sets admin_alert_pace_outlier = true (spec §6.3).
//
// Deploy: supabase functions deploy weekly_pace_zone_update
//
// Required Supabase secrets (supabase secrets set):
//   STRAVA_CLIENT_ID      — Strava app client ID
//   STRAVA_CLIENT_SECRET  — Strava app client secret
//
// Auto-injected by Supabase:
//   SUPABASE_URL              — project REST URL
//   SUPABASE_SERVICE_ROLE_KEY — bypasses RLS for all DB reads/writes

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL         = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY          = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const STRAVA_CLIENT_ID     = Deno.env.get("STRAVA_CLIENT_ID") ?? "";
const STRAVA_CLIENT_SECRET = Deno.env.get("STRAVA_CLIENT_SECRET") ?? "";

const STRAVA_OAUTH_URL = "https://www.strava.com/oauth/token";
const STRAVA_API_URL   = "https://www.strava.com/api/v3";

const MIN_DISTANCE_M       = 3_000;    // spec §4.1: only runs > 3 km
const WINDOW_DAYS          = 90;       // spec §4.1: rolling 90-day window
const WEEKS_IN_WINDOW      = WINDOW_DAYS / 7;
const OUTLIER_THRESHOLD_S  = 120;     // spec §6.3: 2 min/km in seconds
const OUTLIER_RUN_COUNT    = 3;       // spec §6.3: 3+ consecutive runs trigger alert

// ─── Interfaces ─────────────────────────────────────────────────────────────

interface UserRow {
  id: string;
  avg_pace_seconds: number | null;
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

interface StravaActivitySummary {
  sport_type: string;
  distance: number;       // metres
  moving_time: number;    // seconds
  start_date: string;     // ISO 8601 UTC
}

interface RecentAttendedRun {
  actual_avg_pace: number; // sec/km
}

// ─── Pace zone calculation (mirrors shared/paceZone/PaceZoneCalculator.kt) ──

const RUNNING_SPORT_TYPES = new Set([
  "Run", "TrailRun", "VirtualRun", "Treadmill", "RaceRun",
]);

/**
 * Assigns a pace zone label from average sec/km.
 * Boundaries from PaceZone.kt: A<270, B<300, C<330, D<390, E>=390.
 */
function toPaceZone(avgPaceSecPerKm: number): string {
  if (avgPaceSecPerKm < 270) return "A";
  if (avgPaceSecPerKm < 300) return "B";
  if (avgPaceSecPerKm < 330) return "C";
  if (avgPaceSecPerKm < 390) return "D";
  return "E";
}

interface PaceZoneResult {
  zone: string;
  avgPaceSecondsPerKm: number;
  weeklyMileageAvgKm: number;
}

/**
 * Calculates pace zone from raw Strava activity summaries.
 * Returns null when there are no qualifying activities.
 */
function calcPaceZone(activities: StravaActivitySummary[]): PaceZoneResult | null {
  const cutoffMs = Date.now() - WINDOW_DAYS * 24 * 60 * 60 * 1_000;

  const qualifying = activities.filter(
    (a) =>
      RUNNING_SPORT_TYPES.has(a.sport_type) &&
      a.distance >= MIN_DISTANCE_M &&
      new Date(a.start_date).getTime() >= cutoffMs,
  );

  if (qualifying.length === 0) return null;

  let totalDistanceKm = 0;
  let totalTimeSeconds = 0;

  for (const act of qualifying) {
    const distKm = act.distance / 1_000;
    const paceSecPerKm = act.moving_time / distKm;
    totalDistanceKm += distKm;
    totalTimeSeconds += paceSecPerKm * distKm; // time-weighted average
  }

  if (totalDistanceKm <= 0) return null;

  const avgPaceSecondsPerKm = Math.round(totalTimeSeconds / totalDistanceKm);
  const weeklyMileageAvgKm  = totalDistanceKm / WEEKS_IN_WINDOW;

  return {
    zone: toPaceZone(avgPaceSecondsPerKm),
    avgPaceSecondsPerKm,
    weeklyMileageAvgKm,
  };
}

// ─── Strava helpers ──────────────────────────────────────────────────────────

async function refreshStravaToken(
  stored: StoredStravaToken,
): Promise<StravaTokenResponse | null> {
  try {
    const res = await fetch(STRAVA_OAUTH_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        client_id:     STRAVA_CLIENT_ID,
        client_secret: STRAVA_CLIENT_SECRET,
        grant_type:    "refresh_token",
        refresh_token: stored.refresh_token,
      }),
    });
    if (!res.ok) return null;
    return (await res.json()) as StravaTokenResponse;
  } catch {
    return null;
  }
}

/** Returns all Strava running activities from the last 90 days. */
async function fetchStravaActivities(
  accessToken: string,
): Promise<StravaActivitySummary[]> {
  try {
    const afterEpoch = Math.floor((Date.now() - WINDOW_DAYS * 86_400_000) / 1_000);
    const url = new URL(`${STRAVA_API_URL}/athlete/activities`);
    url.searchParams.set("after", String(afterEpoch));
    url.searchParams.set("per_page", "200");

    const res = await fetch(url.toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!res.ok) return [];
    return (await res.json()) as StravaActivitySummary[];
  } catch {
    return [];
  }
}

// ─── Outlier detection (spec §6.3) ──────────────────────────────────────────

/**
 * Returns true if the last OUTLIER_RUN_COUNT consecutive attended runs
 * all deviate by more than OUTLIER_THRESHOLD_S sec/km from the user's
 * pre-update stated avg pace.
 */
function isOutlier(
  recentRuns: RecentAttendedRun[],
  statedPaceSecPerKm: number,
): boolean {
  if (recentRuns.length < OUTLIER_RUN_COUNT) return false;

  const lastN = recentRuns.slice(0, OUTLIER_RUN_COUNT);
  return lastN.every(
    (r) => Math.abs(r.actual_avg_pace - statedPaceSecPerKm) > OUTLIER_THRESHOLD_S,
  );
}

// ─── Main handler ────────────────────────────────────────────────────────────

Deno.serve(async (_req: Request): Promise<Response> => {
  if (!STRAVA_CLIENT_ID || !STRAVA_CLIENT_SECRET) {
    console.error("weekly_pace_zone_update: STRAVA_CLIENT_ID / STRAVA_CLIENT_SECRET not set");
    return new Response(
      JSON.stringify({ error: "missing Strava credentials" }),
      { status: 500 },
    );
  }

  const supabase = createClient(SUPABASE_URL, SERVICE_KEY);

  // Step 1: fetch all Strava-connected users.
  const { data: users, error: usersError } = await supabase
    .from("users")
    .select("id, avg_pace_seconds")
    .eq("strava_connected", true) as { data: UserRow[] | null; error: unknown };

  if (usersError) {
    console.error("weekly_pace_zone_update: failed to fetch users", usersError);
    return new Response(JSON.stringify({ error: String(usersError) }), { status: 500 });
  }

  if (!users || users.length === 0) {
    console.log("weekly_pace_zone_update: no Strava-connected users");
    return new Response(JSON.stringify({ updated: 0, alerted: 0 }), { status: 200 });
  }

  console.log(`weekly_pace_zone_update: processing ${users.length} user(s)`);

  // Step 2: batch-fetch strava tokens.
  const userIds = users.map((u) => u.id);
  const { data: tokenRows } = await supabase
    .from("user_strava_tokens")
    .select("user_id, access_token, refresh_token, expires_at")
    .in("user_id", userIds) as { data: StoredStravaToken[] | null; error: unknown };

  const tokenMap = new Map<string, StoredStravaToken>(
    (tokenRows ?? []).map((t) => [t.user_id, t]),
  );

  let totalUpdated = 0;
  let totalAlerted = 0;
  let totalSkipped = 0;

  for (const user of users) {
    let stored = tokenMap.get(user.id);
    if (!stored) {
      // No Strava token stored — user connected via web or token missing; skip.
      totalSkipped++;
      continue;
    }

    // Step 3: refresh expired token (60 s buffer).
    const nowEpoch = Math.floor(Date.now() / 1_000);
    if (stored.expires_at < nowEpoch + 60) {
      const refreshed = await refreshStravaToken(stored);
      if (!refreshed) {
        console.warn(`weekly_pace_zone_update: token refresh failed for user ${user.id}`);
        totalSkipped++;
        continue;
      }
      await supabase.from("user_strava_tokens").upsert({
        user_id:       user.id,
        access_token:  refreshed.access_token,
        refresh_token: refreshed.refresh_token,
        expires_at:    refreshed.expires_at,
      });
      stored = { ...stored, ...refreshed };
    }

    // Step 4: fetch activities and recalculate pace zone.
    const activities = await fetchStravaActivities(stored.access_token);
    const result     = calcPaceZone(activities);

    if (!result) {
      // No qualifying runs in 90-day window — user has no verifiable pace data; skip update.
      console.log(`weekly_pace_zone_update: no qualifying activities for user ${user.id}`);
      totalSkipped++;
      continue;
    }

    // Step 5: outlier detection — check BEFORE updating stated pace (spec §6.3).
    const statedPace = user.avg_pace_seconds;
    let flagOutlier  = false;

    if (statedPace !== null) {
      const { data: recentRuns } = await supabase
        .from("run_participants")
        .select("actual_avg_pace")
        .eq("user_id", user.id)
        .eq("status", "attended")
        .not("actual_avg_pace", "is", null)
        .order("attended_at", { ascending: false })
        .limit(OUTLIER_RUN_COUNT) as { data: RecentAttendedRun[] | null; error: unknown };

      if (recentRuns && isOutlier(recentRuns, statedPace)) {
        flagOutlier = true;
        console.log(
          `weekly_pace_zone_update: pace outlier detected for user ${user.id} ` +
          `(stated=${statedPace}s/km, new=${result.avgPaceSecondsPerKm}s/km)`,
        );
        totalAlerted++;
      }
    }

    // Step 6: update users table.
    const updatePayload: Record<string, unknown> = {
      pace_zone:           result.zone,
      avg_pace_seconds:    result.avgPaceSecondsPerKm,
      weekly_mileage_avg:  result.weeklyMileageAvgKm,
      updated_at:          new Date().toISOString(),
    };
    if (flagOutlier) updatePayload.admin_alert_pace_outlier = true;

    const { error: updateError } = await supabase
      .from("users")
      .update(updatePayload)
      .eq("id", user.id);

    if (updateError) {
      console.error(`weekly_pace_zone_update: update failed for user ${user.id}`, updateError);
    } else {
      totalUpdated++;
      console.log(
        `weekly_pace_zone_update: updated user ${user.id} ` +
        `zone=${result.zone} pace=${result.avgPaceSecondsPerKm}s/km outlier=${flagOutlier}`,
      );
    }
  }

  console.log(
    `weekly_pace_zone_update: done — updated=${totalUpdated} alerted=${totalAlerted} skipped=${totalSkipped}`,
  );
  return new Response(
    JSON.stringify({ updated: totalUpdated, alerted: totalAlerted, skipped: totalSkipped }),
    { status: 200 },
  );
});
