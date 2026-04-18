# PaceUp — Claude Code Project Context

## Read first
Before touching any code, read ALL of the following in order:
1. .claude/skills/android-module-structure/SKILL.md
2. .claude/skills/android-data-layer/SKILL.md
3. .claude/skills/android-presentation-mvi/SKILL.md
4. .claude/skills/android-navigation/SKILL.md
5. .claude/skills/android-di-koin/SKILL.md
6. .claude/skills/android-testing/SKILL.md
7. .claude/skills/android-error-handling/SKILL.md
8. .claude/skills/android-compose-ui/SKILL.md
9. .claude/skills/ui-design/SKILL.md
10. docs/SPEC.md
11. docs/IMPLEMENTATION_PLAN.md

These define ALL architecture, patterns, naming, and conventions.
Never deviate from them unless this file explicitly overrides.

---
## Implementation plan & task tracking

The full implementation plan is at docs/IMPLEMENTATION_PLAN.md.
It contains every feature in the correct build order, grouped by phase.

### Rules — follow these every session:

1. At the start of every session, read IMPLEMENTATION_PLAN.md and
   identify the current task.

2. Before implementing any task, read the spec section referenced
   in that task. Never guess what a feature should do — check the
   spec first.

3. When a task is fully complete, update IMPLEMENTATION_PLAN.md:
    - Change `- [ ] Done` to `- [x] Done`
    - Add a one-line note below if any important decision was made

4. Never mark a task complete if:
    - It does not compile on Android
    - Tests are missing (if the task requires them)
    - The iOS expect/actual counterpart is not implemented
    - Any critical rule from this file was broken

5. After marking complete, state:
   "Task [ID] complete. Moving to task [next ID]."
   before doing anything else.

---

## KMP/CMP overrides
The skills are Android-focused. PaceUp is Kotlin Multiplatform.
Apply these overrides wherever skills conflict with KMP:

- Shared logic lives in shared/ KMP modules, not Android feature modules
- No Room — use SQLDelight for shared local database
- No Android-specific APIs in shared/ — ever
- No DataStore — SQLDelight handles local persistence
- Ktor HttpClient configured in shared/network (not Android-only)
- Koin starts in composeApp/ for shared code
- androidApp/ and iosApp/ each start Koin with platform-specific additions
- Navigation lives in composeApp/ as Compose Multiplatform
- expect/actual used for GPS, push notifications, OAuth browser, and maps only

---

## Target module structure

```
shared/
├── auth/             # Supabase auth + Strava/Garmin OAuth
├── supabase/         # Supabase REST client
├── realtime/         # Supabase Realtime subscriptions
├── activities/       # Strava/Garmin activity sync
├── paceZone/         # Pace zone calculation algorithm
├── runMatching/      # Run discovery and filtering
├── rivalEngine/      # Weekly rival stats
├── reputationEngine/ # Show-up rate, pace accuracy
├── notifications/    # Notification scheduling logic
├── database/         # SQLDelight local cache
└── network/          # Ktor client + interceptors
```

---

## What this app is
PaceUp is a running social platform. Runners create and join real-world
group runs matched by verified pace data from Strava and Garmin.

Full spec: docs/SPEC.md — check it for feature behaviour, data models,
and business rules. Never guess — always check the spec.

---

## Backend
Supabase — PostgreSQL + Auth + Realtime + Storage + Edge Functions.

Status: NOT YET CREATED.

When writing backend-dependent code:
- Define interfaces and data models correctly
- Use placeholder config values
- Mark with: // TODO: replace with real Supabase config

Supabase URL:https://xlbccbaeaesgfydkajdu.supabase.co
Anon key:eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhsYmNjYmFlYWVzZ2Z5ZGthamR1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzY1MDA0MzksImV4cCI6MjA5MjA3NjQzOX0.JO4dR5HMm-epvFblqLfLg6RC5lpk2tIVtJftt7lwyVU

``
## Critical rules — never break these

- Never store raw Strava/Garmin activity data — derived metrics only
  (avg_pace_seconds, distance_km, date — nothing else)
- All Supabase tables must have RLS policies — never skip
- pace_zone is always server-calculated — never accept user input for it
- RTL layout must work in Hebrew on every new screen
- All UI strings in resource files — never hardcoded
- No business logic in Composables — ViewModels only
- No Java — Kotlin only everywhere
- No callbacks — Coroutines + Flow only

---

## Developer context

- Android developer (mid-level), learning KMP
- Running Claude Code on Windows
- iOS cannot be built locally — write correct KMP code but verify
  compilation on Android only for now
- When making architectural decisions: explain briefly WHY before coding
- Prefer simple and correct over clever and fragile
- When unsure between two approaches: ask, do not guess

---
## UI Design Direction

Style: athletic, clean, data-forward. Strava meets Garmin but more social.
Feels fast and trustworthy — not playful, not corporate.

Color palette:
- Primary: #1D6FA8 (blue — pace, trust)
- Accent: #FC4C02 (Strava orange — energy, Strava connection)
- Success: #0F6E56 (green — attended, on pace)
- Warning: #BA7517 (amber — caution, off pace)
- Error: #A32D2D (red — no-show, banned)
- Background: #F8F9FA (near white)
- Surface: #FFFFFF
- Text primary: #1A1A2E
- Text secondary: #666666

Typography:
- Headlines: bold, tight letter spacing
- Body: regular weight, generous line height
- Stats/numbers: tabular nums, monospace feel for pace display

Components:
- Corner radius: 12dp for cards, 8dp for chips, 24dp for buttons
- Elevation: flat design — borders not shadows
- Pace zone badges: colored pill — A=purple, B=blue, C=green, D=amber, E=gray
- Show-up rate: always shown as colored percentage (green/amber/red)
- Maps: always full-bleed, pins color-coded by pace zone
- Buttons: filled primary for main CTA, outlined for secondary
- Empty states: always have an icon + headline + CTA — never just text

Motion:
- Transitions: 300ms, ease-in-out
- No bouncy animations — this is a sports app, not a game

---
## Developer Rules

### Logging
- Use the shared AppLogger wrapper (shared/network/logger/) — never use
  println, Log.d, or NSLog directly in feature code
- Log levels: d() debug, i() info, w() warning, e() error
- Format: AppLogger.d("ModuleName", "message")
- All logs are stripped in release builds automatically
- Log at entry/exit of every repository function and every API call

### Documentation
- Every public class, function, and data class gets a KDoc comment
- KDoc minimum: one-line description + @param for non-obvious params
- Private functions: comment only if the logic is non-obvious
- Every shared/ module has a README.md explaining its purpose

### iOS Parity — NEVER SKIP THIS
- Every time you fix or implement something that touches expect/actual,
  you MUST implement BOTH the Android and iOS sides
- Every time you use a platform API, verify it exists on both targets
- After every Android implementation, explicitly state:
  "iOS counterpart implemented in iosApp/ at [file path]"
- If iOS cannot be verified locally, add a comment:
  // iOS: implemented in iosApp/[path] — verify on Mac before PR

### API Level & Target Rules
- minSdk is defined in libs.versions.toml — never use APIs below it
- Check @RequiresApi before using any API introduced after minSdk
- Never use Android-specific APIs in shared/ modules — ever
- Use expect/actual for anything platform-specific
- After implementing any feature, ask: "does this work on both targets?"

### Secrets & Security
- NEVER hardcode API keys, URLs, or tokens in source files
- All secrets go in local.properties (already in .gitignore)
- Supabase anon key is the only key allowed in code (it's public by design)
- Service role key: server-side only, never in the app

### TODOs
- Format: // TODO(paceup): description
- All TODOs are searchable with: grep -r "TODO(paceup)" .
- A TODO must have a description — never just // TODO

### Git Commits
- Format: type(scope): description
- Types: feat, fix, refactor, test, docs, chore
- Example: feat(auth): add Strava OAuth flow for Android
- Example: fix(runMatching): correct pace zone filter logic
- Claude Code: always suggest a commit message after completing a task

### Git Branching Strategy
- Never commit directly to `main` or `dev` — always use a feature branch
- Branch naming:
    - Features: `feat/short-description` (e.g. `feat/strava-oauth`)
    - Fixes: `fix/short-description` (e.g. `fix/pace-zone-filter`)
    - Refactors: `refactor/short-description`
- After completing any task, always suggest the full git flow:
    1. `git checkout -b feat/your-feature` (if not already on a branch)
    2. `git add .`
    3. `git commit -m "type(scope): description"`
    4. `git push origin feat/your-feature`
- Never merge branches — that is the developer's decision
- Always remind which branch you're on at the start of a new task

### Branch Hierarchy
- `main` — production only, merged from dev when explicitly instructed
- `dev` — integration branch, merged from feature branches when developer approves
- Feature branches are deleted after merging into dev

### Rules
- If no branch exists yet for the current task, create one before writing any code
- Always state the current branch at the start of each session
- When a task is done, suggest: "Ready to merge into dev when you approve"

### Performance
- No network calls on the main thread — always Dispatchers.IO
- No blocking operations in Composables or ViewModel constructors
- Measure with Android Profiler before optimising — never guess

### Crash Reporting
- Placeholder: CrashReporter.kt exists in shared/network/
- All caught exceptions go through CrashReporter.record(e)
- Firebase Crashlytics will be wired here in Phase 2

---
## Project status

- [ ] Skills read and understood
- [ ] KMP module structure scaffolded
- [ ] Gradle version catalog set up
- [ ] Supabase client placeholder in shared/supabase
- [ ] Koin initialized for CMP
- [ ] Navigation skeleton working
- [ ] Login screen UI shell
- [ ] Strava connect screen UI shell
- [ ] Compiles and runs on Android emulator
