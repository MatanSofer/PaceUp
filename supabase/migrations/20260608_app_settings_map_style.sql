-- Migration: preferred_map_style column on users (spec §5.2 App)
ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS preferred_map_style TEXT NOT NULL DEFAULT 'standard';

ALTER TABLE public.users
  ADD CONSTRAINT chk_preferred_map_style CHECK (preferred_map_style IN ('standard', 'satellite'));

COMMENT ON COLUMN public.users.preferred_map_style IS 'standard | satellite';
