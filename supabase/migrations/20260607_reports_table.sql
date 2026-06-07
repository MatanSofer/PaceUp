-- ─── reports table ────────────────────────────────────────────────────────────
-- Stores user/run/message reports for admin moderation queue. Spec §6.2.
CREATE TABLE IF NOT EXISTS public.reports (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id         UUID        NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    reported_user_id    UUID        REFERENCES public.users(id) ON DELETE SET NULL,
    reported_run_id     UUID        REFERENCES public.runs(id) ON DELETE SET NULL,
    -- For message reports, sender stored in reported_user_id; content in description.
    report_type         TEXT        NOT NULL
                                    CHECK (report_type IN ('user', 'run', 'message')),
    reason              TEXT        NOT NULL,
    description         TEXT,
    status              TEXT        NOT NULL DEFAULT 'pending'
                                    CHECK (status IN ('pending','reviewed','actioned','dismissed')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;

-- Authenticated users may submit reports
CREATE POLICY "reports_insert_own"
    ON public.reports FOR INSERT
    WITH CHECK (reporter_id = auth.uid());

-- Reporters can view their own submissions
CREATE POLICY "reports_select_own"
    ON public.reports FOR SELECT
    USING (reporter_id = auth.uid());

-- No UPDATE or DELETE for regular users — admin reads via service role only.
