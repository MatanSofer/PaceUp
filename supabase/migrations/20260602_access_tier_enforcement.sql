-- Migration: access tier enforcement (spec §4.4)
-- Prevents new_runner users from creating runs at the database level.
-- new_runner = users with fewer than 3 attended runs (reputation_tier = 'new_runner').

-- Prevents new_runners from creating runs
CREATE POLICY "active users can create runs" ON public.runs
    FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.users
            WHERE id = auth.uid()
              AND reputation_tier IN ('active', 'trusted', 'pacer_eligible')
        )
    );

-- new_runner can only join open runs (non-open join_mode blocked at DB level)
CREATE POLICY "new_runner can only join open runs" ON public.run_participants
    FOR INSERT
    WITH CHECK (
        NOT (
            (SELECT reputation_tier FROM public.users WHERE id = auth.uid()) = 'new_runner'
            AND
            (SELECT join_mode FROM public.runs WHERE id = run_id) != 'open'
        )
    );
