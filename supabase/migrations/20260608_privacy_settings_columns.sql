-- Migration: privacy settings columns on users (spec §5.2 Privacy)
ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS profile_visibility  TEXT    NOT NULL DEFAULT 'public',
  ADD COLUMN IF NOT EXISTS show_pace_zone      BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS show_run_history    BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS show_rivals         BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS location_precision  TEXT    NOT NULL DEFAULT 'city';

-- Constrain allowed values
ALTER TABLE public.users
  ADD CONSTRAINT chk_profile_visibility CHECK (profile_visibility IN ('public', 'friends', 'private')),
  ADD CONSTRAINT chk_location_precision CHECK (location_precision IN ('city', 'country'));

COMMENT ON COLUMN public.users.profile_visibility IS 'public | friends | private';
COMMENT ON COLUMN public.users.location_precision  IS 'city | country';
