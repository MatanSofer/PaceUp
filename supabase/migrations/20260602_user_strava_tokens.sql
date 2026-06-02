-- Migration: user_strava_tokens table
-- Stores Strava OAuth tokens per user, separate from public.users to avoid
-- exposing tokens through the public-readable users row (spec §14.1 — tokens protected).
-- service_role (used by edge functions) bypasses RLS and can read all rows.

CREATE TABLE public.user_strava_tokens (
  user_id       uuid    PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
  access_token  text    NOT NULL,
  refresh_token text    NOT NULL,
  expires_at    bigint  NOT NULL,  -- Unix epoch seconds (Strava format)
  updated_at    timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.user_strava_tokens ENABLE ROW LEVEL SECURITY;

-- Owner can read their own token (e.g., for client-side token refresh UI)
CREATE POLICY "strava_tokens_owner_select" ON public.user_strava_tokens
  FOR SELECT USING (auth.uid() = user_id);

-- Owner can insert their token (first connect)
CREATE POLICY "strava_tokens_owner_insert" ON public.user_strava_tokens
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Owner can update their token (reconnect / refresh)
CREATE POLICY "strava_tokens_owner_update" ON public.user_strava_tokens
  FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- Owner can delete their token (disconnect Strava from Settings)
CREATE POLICY "strava_tokens_owner_delete" ON public.user_strava_tokens
  FOR DELETE USING (auth.uid() = user_id);

-- Keep updated_at current on every row update
CREATE OR REPLACE FUNCTION update_strava_tokens_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

CREATE TRIGGER strava_tokens_set_updated_at
  BEFORE UPDATE ON public.user_strava_tokens
  FOR EACH ROW EXECUTE FUNCTION update_strava_tokens_updated_at();
