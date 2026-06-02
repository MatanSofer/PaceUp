-- Migration: chat_cleanup helpers
-- Provides the SQL function used by the chat_cleanup Edge Function to find
-- runs whose chat window has expired (48h after run end time, spec §4.6).

create or replace function get_stale_chat_run_ids(archive_hours int default 48)
returns table(id uuid)
language sql
security definer
as $$
  select r.id
  from public.runs r
  where
    -- Run must be completed or in a terminal state
    r.status in ('completed', 'cancelled', 'in_progress')
    -- End time: scheduled_at + (duration_min or 60 minutes) + archive window
    and (r.scheduled_at + ((coalesce(r.duration_min, 60) + archive_hours * 60) * interval '1 minute')) < now()
    -- Only include runs that still have chat messages
    and exists (
      select 1 from public.run_chat_messages m where m.run_id = r.id
    );
$$;
