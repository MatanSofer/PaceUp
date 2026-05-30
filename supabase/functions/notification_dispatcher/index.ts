// notification_dispatcher — Supabase Edge Function
// Polls the notifications table for unsent records due now, sends each via FCM, marks as sent.
// Deploy: supabase functions deploy notification_dispatcher
// Schedule: set in Supabase dashboard → Edge Functions → Schedule (cron: "* * * * *" = every minute)
//
// Required secrets (set via: supabase secrets set FIREBASE_SERVER_KEY=<value>):
//   FIREBASE_SERVER_KEY — Firebase Cloud Messaging legacy server key
//                         (Firebase Console → Project Settings → Cloud Messaging → Server key)

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const FIREBASE_SERVER_KEY = Deno.env.get("FIREBASE_SERVER_KEY")!;
const FCM_URL = "https://fcm.googleapis.com/fcm/send";

interface NotificationRow {
  id: string;
  user_id: string;
  title: string;
  body: string;
  data: Record<string, string> | null;
}

interface UserRow {
  push_token: string | null;
}

Deno.serve(async (_req) => {
  const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY);

  // Fetch all unsent notifications scheduled for now or earlier
  const { data: notifications, error: fetchError } = await supabase
    .from("notifications")
    .select("id, user_id, title, body, data")
    .eq("sent", false)
    .lte("scheduled_at", new Date().toISOString())
    .limit(100);

  if (fetchError) {
    console.error("Failed to fetch notifications:", fetchError.message);
    return new Response(JSON.stringify({ error: fetchError.message }), { status: 500 });
  }

  if (!notifications || notifications.length === 0) {
    return new Response(JSON.stringify({ dispatched: 0 }), { status: 200 });
  }

  let dispatched = 0;
  let failed = 0;

  for (const notification of notifications as NotificationRow[]) {
    // Get the recipient's push token
    const { data: userData, error: userError } = await supabase
      .from("users")
      .select("push_token")
      .eq("id", notification.user_id)
      .single();

    if (userError || !userData) {
      console.warn(`User not found for notification ${notification.id}`);
      failed++;
      continue;
    }

    const user = userData as UserRow;
    if (!user.push_token) {
      console.warn(`No push_token for user ${notification.user_id} — skipping`);
      // Still mark as sent so we don't retry indefinitely for users without tokens
      await supabase
        .from("notifications")
        .update({ sent: true, sent_at: new Date().toISOString() })
        .eq("id", notification.id);
      continue;
    }

    // Send via FCM legacy HTTP API
    const fcmPayload = {
      to: user.push_token,
      notification: {
        title: notification.title,
        body: notification.body,
      },
      data: notification.data ?? {},
    };

    const fcmResponse = await fetch(FCM_URL, {
      method: "POST",
      headers: {
        "Authorization": `key=${FIREBASE_SERVER_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(fcmPayload),
    });

    if (fcmResponse.ok) {
      const fcmResult = await fcmResponse.json();
      if (fcmResult.failure > 0) {
        console.error(`FCM rejected token for notification ${notification.id}:`, fcmResult.results);
        failed++;
      } else {
        // Mark as sent
        await supabase
          .from("notifications")
          .update({ sent: true, sent_at: new Date().toISOString() })
          .eq("id", notification.id);
        dispatched++;
      }
    } else {
      console.error(`FCM request failed for notification ${notification.id}: ${fcmResponse.status}`);
      failed++;
    }
  }

  console.log(`notification_dispatcher: dispatched=${dispatched} failed=${failed}`);
  return new Response(JSON.stringify({ dispatched, failed }), { status: 200 });
});
