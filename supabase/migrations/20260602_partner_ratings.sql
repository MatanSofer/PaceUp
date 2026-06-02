-- Migration: partner_ratings table (spec §4.4)
-- Stores crowdsourced partner tags submitted by attendees after a shared run.
-- One row per (run, rater, rated_user) pair; UNIQUE constraint prevents duplicates.

CREATE TABLE IF NOT EXISTS public.partner_ratings (
    id            uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
    run_id        uuid        NOT NULL REFERENCES public.runs(id)  ON DELETE CASCADE,
    rater_id      uuid        NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    rated_user_id uuid        NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    tags          text[]      NOT NULL DEFAULT '{}',
    created_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT partner_ratings_unique UNIQUE (run_id, rater_id, rated_user_id)
);

CREATE INDEX IF NOT EXISTS partner_ratings_run_rater_idx
    ON public.partner_ratings (run_id, rater_id);

ALTER TABLE public.partner_ratings ENABLE ROW LEVEL SECURITY;

-- Rater inserts only their own rows
CREATE POLICY "rater insert own ratings" ON public.partner_ratings
    FOR INSERT
    WITH CHECK (rater_id = auth.uid());

-- Rater reads their own submissions (used for hasRatedRun and getPartnersToRate)
CREATE POLICY "rater read own ratings" ON public.partner_ratings
    FOR SELECT
    USING (rater_id = auth.uid());

-- Rated user reads ratings they received (future profile display)
CREATE POLICY "rated user reads own ratings" ON public.partner_ratings
    FOR SELECT
    USING (rated_user_id = auth.uid());
