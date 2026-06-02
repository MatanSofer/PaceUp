-- Migration: run_cancellation_notify trigger
-- Fires the run_cancellation_notify Edge Function via pg_net whenever a run's
-- status is updated to 'cancelled' by the creator. The function notifies all
-- accepted participants (spec §8.4).
--
-- Requires: pg_net extension (enabled on all Supabase projects by default).

-- Function called by the trigger
create or replace function notify_run_cancellation()
returns trigger
language plpgsql
security definer
as $$
declare
  function_url text;
begin
  -- Only fire when status transitions TO 'cancelled'
  if NEW.status = 'cancelled' and (OLD.status is null or OLD.status <> 'cancelled') then
    function_url := current_setting('app.supabase_url', true)
      || '/functions/v1/run_cancellation_notify';

    perform net.http_post(
      url     := function_url,
      body    := json_build_object(
        'run_id',               NEW.id,
        'title',                NEW.title,
        'cancellation_reason',  NEW.cancellation_reason
      )::text,
      headers := json_build_object(
        'Content-Type',   'application/json',
        'Authorization',  'Bearer ' || current_setting('app.service_role_key', true)
      )
    );
  end if;
  return NEW;
end;
$$;

-- Drop existing trigger if present to allow re-running this migration
drop trigger if exists on_run_cancelled on public.runs;

create trigger on_run_cancelled
  after update on public.runs
  for each row
  execute function notify_run_cancellation();
