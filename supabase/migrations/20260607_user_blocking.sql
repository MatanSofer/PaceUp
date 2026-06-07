-- ─── user_blocks table ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.user_blocks (
    blocker_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    blocked_id  UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (blocker_id, blocked_id),
    CONSTRAINT no_self_block CHECK (blocker_id != blocked_id)
);

ALTER TABLE public.user_blocks ENABLE ROW LEVEL SECURITY;

CREATE POLICY "blocks_select_own"
    ON public.user_blocks FOR SELECT
    USING (blocker_id = auth.uid());

CREATE POLICY "blocks_insert_own"
    ON public.user_blocks FOR INSERT
    WITH CHECK (blocker_id = auth.uid() AND blocked_id != auth.uid());

CREATE POLICY "blocks_delete_own"
    ON public.user_blocks FOR DELETE
    USING (blocker_id = auth.uid());

-- ─── Mutual block check (SECURITY DEFINER bypasses RLS) ─────────────────────
CREATE OR REPLACE FUNCTION public.fn_is_blocked(a uuid, b uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.user_blocks
        WHERE (blocker_id = a AND blocked_id = b)
           OR (blocker_id = b AND blocked_id = a)
    );
$$;

-- ─── Blocked-users list for the calling user ────────────────────────────────
CREATE OR REPLACE FUNCTION public.fn_get_blocked_users()
RETURNS TABLE (
    id           uuid,
    display_name text,
    avatar_url   text,
    pace_zone    text,
    show_up_rate real
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT u.id, u.display_name, u.avatar_url, u.pace_zone, u.show_up_rate
    FROM   public.user_blocks b
    JOIN   public.users u ON u.id = b.blocked_id
    WHERE  b.blocker_id = auth.uid()
    ORDER  BY b.created_at DESC;
$$;

-- ─── Mutual invisibility — RESTRICTIVE policies ──────────────────────────────
-- RESTRICTIVE policies AND with every PERMISSIVE SELECT policy on the table,
-- so blocked users are invisible in run discovery and user lookups.
CREATE POLICY "runs_block_filter"
    ON public.runs
    AS RESTRICTIVE FOR SELECT
    USING (NOT public.fn_is_blocked(auth.uid(), creator_id));

CREATE POLICY "users_block_filter"
    ON public.users
    AS RESTRICTIVE FOR SELECT
    USING (
        id = auth.uid()                           -- viewer always sees their own row
        OR NOT public.fn_is_blocked(auth.uid(), id)
    );

-- NOTE: chat_messages block filter added separately once that table exists.
