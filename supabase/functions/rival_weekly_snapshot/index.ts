// rival_weekly_snapshot — Supabase Edge Function
// Scheduled via pg_cron every Monday at 04:00 UTC (see migration: rival_weekly_snapshot).
// For every active rival pair:
//   1. Fetches last week's Strava activities for both users (Mon–Sun UTC).
//   2. Calculates: total km, run count, best pace per user.
//   3. Determines winner (most km wins; equal km = tied).
//   4. Upserts a row into rival_weekly_snapshots.
//   5. Updates rivals: increments winner's win count, updates current_streak + streak_count.
//
// "Last week" is defined as the 7-day window from last Monday 00:00 UTC
// to this Monday 00:00 UTC (the moment the function fires on Monday at 04:00 UTC).
//
// Deploy: supabase functions deploy rival_weekly_snapshot
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

const MIN_DISTANCE_M = 0; // include all runs for the rival snapshot (spec says "distance")

// ─── Types ───────────────────────────────────────────────────────────────────

interface Rival {
  id: string;
  user_a_id: string;
  user_b_id: string;
  user_a_wins: number;
  user_b_wins: number;
  current_streak: string; // "user_a" | "user_b" | "tied"
  streak_count: number;
}

interface StoredStravaToken {
  user_id: string;
  access_token: string;
  refresh_token: string;
  expires_at: number;
}

interface StravaTokenResponse {
  access_token: string;
  refresh_token: string;
  expires_at: number;
}

interface StravaActivitySummary {
  sport_type: string;
  distance: number;    // metres
  moving_time: number; // seconds
  start_date: string;  // ISO 8601 UTC
}

interface WeeklyStats {
  totalKm: number;
  runCount: number;
  bestPaceSecPerKm: number | null; // null = no runs
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

const RUNNING_SPORT_TYPES = new Set([
  "Run", "TrailRun", "VirtualRun", "Treadmill", "RaceRun",
]);

/**
 * Returns the Monday 00:00 UTC at the start of the current ISO week.
 * Works correctly even if called on a day other than Monday.
 */
function getThisMonday(): Date {
  const now = new Date();
  const dayOfWeek = now.getUTCDay(); // 0 = Sunday
  const daysToMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
  const monday = new Date(now);
  monday.setUTCDate(monday.getUTCDate() - daysToMonday);
  monday.setUTCHours(0, 0, 0, 0);
  return monday;
}

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

async function fetchWeekActivities(
  accessToken: string,
  afterEpoch: number,
  beforeEpoch: number,
): Promise<StravaActivitySummary[]> {
  try {
    const url = new URL(`${STRAVA_API_URL}/athlete/activities`);
    url.searchParams.set("after", String(afterEpoch));
    url.searchParams.set("before", String(beforeEpoch));
    url.searchParams.set("per_page", "50");
    const res = await fetch(url.toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!res.ok) return [];
    return (await res.json()) as StravaActivitySummary[];
  } catch {
    return [];
  }
}

function calcWeeklyStats(activities: StravaActivitySummary[]): WeeklyStats {
  const runs = activities.filter((a) => RUNNING_SPORT_TYPES.has(a.sport_type));
  if (runs.length === 0) return { totalKm: 0, runCount: 0, bestPaceSecPerKm: null };

  let totalKm = 0;
  let bestPace: number | null = null;

  for (const run of runs) {
    const km = run.distance / 1_000;
    totalKm += km;

    if (km > 0) {
      const paceSecPerKm = run.moving_time / km;
      if (bestPace === null || paceSecPerKm < bestPace) {
        bestPace = paceSecPerKm;
      }
    }
  }

  return {
    totalKm,
    runCount: runs.length,
    bestPaceSecPerKm: bestPace !== null ? Math.round(bestPace) : null,
  };
}

/** Returns "user_a" | "user_b" | "tied" */
function determineWinner(aKm: number, bKm: number): string {
  if (aKm > bKm) return "user_a";
  if (bKm > aKm) return "user_b";
  return "tied";
}

// ─── Main handler ────────────────────────────────────────────────────────────

Deno.serve(async (_req: Request): Promise<Response> => {
  if (!STRAVA_CLIENT_ID || !STRAVA_CLIENT_SECRET) {
    console.error("rival_weekly_snapshot: STRAVA credentials not set");
    return new Response(JSON.stringify({ error: "missing Strava credentials" }), { status: 500 });
  }

  const supabase = createClient(SUPABASE_URL, SERVICE_KEY);

  // Determine the last week's date range.
  const thisMonday  = getThisMonday();
  const lastMonday  = new Date(thisMonday);
  lastMonday.setUTCDate(lastMonday.getUTCDate() - 7);

  const afterEpoch  = Math.floor(lastMonday.getTime()  / 1_000);
  const beforeEpoch = Math.floor(thisMonday.getTime()  / 1_000);
  const weekStart   = lastMonday.toISOString().slice(0, 10); // "YYYY-MM-DD"

  console.log(`rival_weekly_snapshot: processing week=${weekStart}`);

  // Step 1: fetch all active rival pairs.
  const { data: rivals, error: rivalsError } = await supabase
    .from("rivals")
    .select("id, user_a_id, user_b_id, user_a_wins, user_b_wins, current_streak, streak_count")
    .eq("status", "active") as { data: Rival[] | null; error: unknown };

  if (rivalsError) {
    console.error("rival_weekly_snapshot: fetch rivals failed", rivalsError);
    return new Response(JSON.stringify({ error: String(rivalsError) }), { status: 500 });
  }

  if (!rivals || rivals.length === 0) {
    console.log("rival_weekly_snapshot: no active rivals");
    return new Response(JSON.stringify({ processed: 0 }), { status: 200 });
  }

  // Step 2: collect unique user IDs and batch-fetch their Strava tokens.
  const userIds = [...new Set(rivals.flatMap((r) => [r.user_a_id, r.user_b_id]))];
  const { data: tokenRows } = await supabase
    .from("user_strava_tokens")
    .select("user_id, access_token, refresh_token, expires_at")
    .in("user_id", userIds) as { data: StoredStravaToken[] | null; error: unknown };

  const tokenMap = new Map<string, StoredStravaToken>(
    (tokenRows ?? []).map((t) => [t.user_id, t]),
  );

  // Step 3: pre-fetch + refresh tokens and fetch weekly stats per unique user.
  const statsCache = new Map<string, WeeklyStats>();
  const nowEpoch   = Math.floor(Date.now() / 1_000);

  for (const userId of userIds) {
    let stored = tokenMap.get(userId);
    if (!stored) {
      statsCache.set(userId, { totalKm: 0, runCount: 0, bestPaceSecPerKm: null });
      continue;
    }

    // Refresh token if expiring.
    if (stored.expires_at < nowEpoch + 60) {
      const refreshed = await refreshStravaToken(stored);
      if (!refreshed) {
        console.warn(`rival_weekly_snapshot: token refresh failed for user ${userId}`);
        statsCache.set(userId, { totalKm: 0, runCount: 0, bestPaceSecPerKm: null });
        continue;
      }
      await supabase.from("user_strava_tokens").upsert({
        user_id:       userId,
        access_token:  refreshed.access_token,
        refresh_token: refreshed.refresh_token,
        expires_at:    refreshed.expires_at,
      });
      stored = { ...stored, ...refreshed };
    }

    const activities = await fetchWeekActivities(stored.access_token, afterEpoch, beforeEpoch);
    statsCache.set(userId, calcWeeklyStats(activities));
  }

  let totalProcessed = 0;

  // Step 4: process each rival pair.
  for (const rival of rivals) {
    const statsA = statsCache.get(rival.user_a_id) ?? { totalKm: 0, runCount: 0, bestPaceSecPerKm: null };
    const statsB = statsCache.get(rival.user_b_id) ?? { totalKm: 0, runCount: 0, bestPaceSecPerKm: null };

    const winner = determineWinner(statsA.totalKm, statsB.totalKm);

    // Upsert snapshot (idempotent — same week_start + rival_id = same row).
    const { error: snapError } = await supabase
      .from("rival_weekly_snapshots")
      .upsert(
        {
          rival_id:         rival.id,
          week_start:       weekStart,
          user_a_km:        statsA.totalKm,
          user_b_km:        statsB.totalKm,
          user_a_runs:      statsA.runCount,
          user_b_runs:      statsB.runCount,
          user_a_best_pace: statsA.bestPaceSecPerKm,
          user_b_best_pace: statsB.bestPaceSecPerKm,
          winner,
        },
        { onConflict: "rival_id,week_start" },
      );

    if (snapError) {
      console.error(`rival_weekly_snapshot: upsert failed for rival ${rival.id}`, snapError);
      continue;
    }

    // Update win counts and streak.
    let newUserAWins  = rival.user_a_wins;
    let newUserBWins  = rival.user_b_wins;
    let newStreak     = winner;
    let newStreakCount = 0;

    if (winner === "user_a") {
      newUserAWins += 1;
      newStreakCount = rival.current_streak === "user_a" ? rival.streak_count + 1 : 1;
    } else if (winner === "user_b") {
      newUserBWins += 1;
      newStreakCount = rival.current_streak === "user_b" ? rival.streak_count + 1 : 1;
    }
    // tied → newStreak = "tied", newStreakCount = 0

    await supabase
      .from("rivals")
      .update({
        user_a_wins:    newUserAWins,
        user_b_wins:    newUserBWins,
        current_streak: newStreak,
        streak_count:   newStreakCount,
        updated_at:     new Date().toISOString(),
      })
      .eq("id", rival.id);

    console.log(
      `rival_weekly_snapshot: processed rival=${rival.id} ` +
      `A=${statsA.totalKm.toFixed(1)}km B=${statsB.totalKm.toFixed(1)}km winner=${winner}`,
    );
    totalProcessed++;
  }

  console.log(`rival_weekly_snapshot: done — processed=${totalProcessed}/${rivals.length}`);
  return new Response(
    JSON.stringify({ processed: totalProcessed, week: weekStart }),
    { status: 200 },
  );
});
