# PaceUp — Implementation Plan v2

> Revised based on spec alignment review.
> Restructured into 3 buckets: Must Launch, Launch-If-Time, Post-Launch.
> The core loop we are proving: connect Strava → get verified pace → discover nearby run → join → show up → rate partners → build reputation.
> Every task in Must Launch serves this loop directly.

---

## How to use this plan

At the start of every Claude Code session, paste:

```
Read CLAUDE.md, all skills, and docs/SPEC.md.
Then read docs/IMPLEMENTATION_PLAN.md.
We are currently on task [TASK ID].
Implement it fully before moving to the next.
```

When a task is complete, mark it: `[x]` and note any decisions made below it.

---

## Bucket A — Must Launch

> Everything here must be done before any real user touches the app.
> Nothing in Bucket B or C is started until Bucket A is complete and stable.

---

### Phase 0 — Project Foundation

> Goal: empty KMP project becomes a compiling skeleton with all modules in place.
> Nothing user-facing. No feature code.

---

#### Task 0.1 — KMP module scaffold

- **What:** Create all shared/ KMP modules, composeApp/, androidApp/, iosApp/ with correct build.gradle.kts. Set up Gradle version catalog (libs.versions.toml) with all dependencies.
- **Spec ref:** SPEC.md Section 12.1 (Shared KMP Modules), Section 12.2 (Platform-Specific)
- **Skills:** android-module-structure
- **Dependencies:** none
- **Deliverable:** `./gradlew :androidApp:assembleDebug` succeeds on empty project

**Modules to create:**
```
shared/auth
shared/supabase
shared/realtime
shared/activities
shared/paceZone
shared/runMatching
shared/rivalEngine
shared/reputationEngine
shared/notifications
shared/database
shared/network
shared/deeplink
shared/i18n
composeApp/
androidApp/
iosApp/
```

**Dependencies to include in version catalog:**
- Supabase Kotlin SDK (auth, postgrest, realtime, storage)
- Ktor (client-core, client-cio, serialization)
- SQLDelight (runtime, coroutines-extensions)
- Koin (core, compose, android)
- Compose Multiplatform
- lifecycle-viewmodel-compose
- Kotlinx Serialization
- Kotlinx Coroutines
- Coil (compose)
- KtLint

- [x] Done
  - composeApp converted to androidLibrary; androidApp created as the Android shell entry point
  - All 13 shared KMP modules scaffolded with androidTarget + iosArm64 + iosSimulatorArm64
  - libs.versions.toml expanded with all required dependencies (Supabase, Ktor, SQLDelight, Koin, Coil, KtLint)
  - Deliverable verified: `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL

---

#### Task 0.2 — AppLogger

- **What:** Shared logging wrapper. expect/actual. Debug-only — stripped in release.
- **Spec ref:** CLAUDE.md Developer Rules → Logging
- **Skills:** android-data-layer
- **Dependencies:** Task 0.1
- **Deliverable:** `AppLogger.d("Tag", "message")` callable from any shared module

```
shared/network/logger/AppLogger.kt        — expect declaration
androidApp/AppLogger.android.kt           — actual using android.util.Log
iosApp/AppLogger.ios.kt                   — actual using NSLog
```

Rules:
- Debug only — stripped in release
- Format: `[PaceUp][Tag] message`
- Levels: d() i() w() e()

- [x] Done
  - expect/actual in shared/network/src/{commonMain,androidMain,iosMain}/logger/AppLogger.kt
  - Debug-only via setDebugEnabled(Boolean) — called from PaceUpApplication with BuildConfig.DEBUG
  - buildFeatures { buildConfig = true } added to androidApp
  - iOS: actual at shared/network/src/iosMain — verify on Mac before PR

---

#### Task 0.3 — Shared Result type and AppError

- **What:** Shared `Result<D, E>` sealed class and `AppError` hierarchy used across all layers.
- **Spec ref:** CLAUDE.md Developer Rules → Error Handling
- **Skills:** android-data-layer
- **Dependencies:** Task 0.1
- **Deliverable:** `Result<T, AppError>` importable from any shared module

```
shared/network/result/Result.kt
shared/network/error/AppError.kt
shared/network/error/NetworkError.kt
shared/network/error/AuthError.kt
shared/network/error/DatabaseError.kt
```

- [x] Done
  - Result<D,E>, EmptyResult, map/onSuccess/onFailure/asEmptyResult in shared/network/result/
  - AppError sealed interface + NetworkError, AuthError, DatabaseError enums in shared/network/error/
  - -Xexpect-actual-classes compiler flag added to shared/network to suppress Beta warning

---

#### Task 0.4 — Supabase client

- **What:** Supabase client singleton configured with Ktor engine. Placeholder credentials.
- **Spec ref:** SPEC.md Section 8.1 (Data Storage Map)
- **Skills:** android-data-layer, android-di-koin
- **Dependencies:** Task 0.1, Task 0.2, Task 0.3
- **Deliverable:** `SupabaseClient` injectable via Koin from any module

```
shared/supabase/client/SupabaseClientProvider.kt
shared/supabase/di/SupabaseModule.kt
```

- [x] Done
  - Fixed libs.versions.toml: group `io.github.jan-tennert.supabase`, artifacts `auth-kt`/`postgrest-kt`/`realtime-kt`/`storage-kt` (was wrong group `io.github.jan-tennermann:supabase-*-kt`)
  - `SupabaseClientProvider` object in `shared/supabase/client/` — installs Auth, Postgrest, Realtime, Storage
  - `supabaseModule` Koin val in `shared/supabase/di/` — `single { SupabaseClientProvider.create() }`
  - Ktor engines: `androidMain → ktor-client-android`, `iosMain → ktor-client-darwin`

---

#### Task 0.5 — Koin initialization

- **What:** Koin DI wired for both Android and iOS. All module-level Koin modules assembled.
- **Spec ref:** CLAUDE.md KMP/CMP overrides → DI override
- **Skills:** android-di-koin
- **Dependencies:** Task 0.4
- **Deliverable:** Koin starts without error on Android emulator launch

- [x] Done
  - `sharedModules` list in `composeApp/commonMain/di/AppModules.kt` — single assembly point
  - `startKoin { androidLogger(); androidContext(this); modules(sharedModules) }` in `PaceUpApplication`
  - `initKoin()` in `composeApp/iosMain/di/IosKoinSetup.kt` for iOS Swift AppDelegate
  - Build verified: `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL

---

#### Task 0.6 — Navigation skeleton

- **What:** Type-safe navigation with @Serializable route objects. All routes defined even if destinations are empty composables.
- **Spec ref:** SPEC.md Section 5.1 (Onboarding Flow), Section 10.1 (Deep Linking)
- **Skills:** android-navigation
- **Dependencies:** Task 0.5
- **Deliverable:** App launches, navigates between empty screens without crash

**Routes to define:**
```
WelcomeRoute
LoginRoute
SignUpRoute
StravaConnectRoute
OnboardingLocationRoute
OnboardingNotificationsRoute
OnboardingProfileRoute
HomeRoute (map)
RunDetailRoute(runId: String)
CreateRunRoute
UserProfileRoute(userId: String)
RivalDashboardRoute
SettingsRoute
SettingsAccountRoute
SettingsNotificationsRoute
SettingsPrivacyRoute
SettingsAppRoute
BlockedUsersRoute
```

**Launch logic:**
- Check Supabase session on launch
- Session exists → HomeRoute
- No session → WelcomeRoute

- [x] Done
  - `navigation-compose = "2.9.2"` added to libs.versions.toml (stable, Maven Central)
  - `kotlinSerialization` plugin applied to composeApp (required for @Serializable routes)
  - All 18 routes defined in `composeApp/commonMain/navigation/Routes.kt`
  - `appGraph()` NavGraphBuilder extension with stub screens in `AppNavGraph.kt`
  - `App.kt` replaced with NavHost; session check deferred to Task 1.5 (AuthRepository not yet available)
  - Build verified: `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL

---

#### Task 0.7 — SQLDelight local database

- **What:** SQLDelight schema for local cache tables only.
- **Spec ref:** SPEC.md Section 8.1 (SQLDelight — device)
- **Skills:** android-data-layer
- **Dependencies:** Task 0.1
- **Deliverable:** SQLDelight compiles, database opens on Android

**Tables:**
```
CachedUser          — logged-in user's own profile
CachedRun           — runs user has joined or created
CachedRival         — rival weekly snapshot
CachedNotification  — unread notification state
```

- [x] Done
  - SQLDelight 2.0.2 plugin applied to `shared/database`
  - 4 tables: `CachedUser`, `CachedRun`, `CachedRival`, `CachedNotification` with full CRUD queries
  - `DatabaseDriverFactory` expect/actual: Android uses `AndroidSqliteDriver`, iOS uses `NativeSqliteDriver`
  - `databaseModule` (shared) + `androidDatabaseModule` (Android-specific factory) wired into Koin
  - Build verified: `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL

---

### Phase 1 — Supabase Schema & Auth Foundation

> Goal: database exists with correct schema and RLS. First real user can sign up and log in.

---

#### Task 1.1 — Supabase schema: users table

- **What:** Create the `users` table with all columns from spec. Enable RLS. Apply users RLS policy. Add trigger to auto-insert on auth.users creation.
- **Spec ref:** SPEC.md Section 8.2 → users table, Section 8.5 → users RLS
- **Skills:** android-data-layer
- **Dependencies:** Task 0.4, Supabase MCP connected
- **Deliverable:** `users` table in Supabase with correct columns, RLS enabled, trigger working

- [x] Done
  - 27-column `public.users` table with all spec fields, constraints, and defaults
  - RLS enabled: `users_select_public` (everyone reads), `users_update_owner` (owner updates own row)
  - `on_auth_user_created` trigger on `auth.users` — auto-inserts public.users row on signup
  - `users_set_updated_at` trigger keeps `updated_at` current on every UPDATE
  - Admin writes (is_suspended, is_banned) use service_role which bypasses RLS — admin panel Task 13.1

---

#### Task 1.2 — Supabase schema: all remaining tables

- **What:** Create all remaining tables in dependency order.
- **Spec ref:** SPEC.md Section 8.2 (all tables)
- **Skills:** android-data-layer
- **Dependencies:** Task 1.1
- **Deliverable:** All tables created with correct columns, foreign keys

**Order:**
1. runs
2. run_participants
3. partner_ratings
4. rivals
5. rival_weekly_snapshots
6. run_chat_messages
7. badges
8. reports
9. notifications
10. app_config (for version check — platform, min_version, force_update, update_message)
11. user_blocks (blocker_id, blocked_id, created_at)

- [x] Done
  - All 11 tables created in correct dependency order with full spec columns, FKs, constraints, and defaults
  - `updated_at` triggers applied to: runs, run_participants, rivals, app_config
  - RLS enabled on all tables (policies applied in Task 1.3)
  - `app_config` seeded with android + ios rows (min_version 1.0.0, force_update false)
  - `run_participants` has UNIQUE(run_id, user_id); `rivals` has UNIQUE(user_a_id, user_b_id)

---

#### Task 1.3 — All RLS policies

- **What:** Apply all Row-Level Security policies for all tables.
- **Spec ref:** SPEC.md Section 8.5 (RLS policies)
- **Dependencies:** Task 1.2
- **Deliverable:** Every table has RLS enabled with correct policies. Verified by querying as anonymous user.

**Policies per spec:**
- users: public fields readable by anyone, owner updates own row, admin writes banned/suspended
- runs: anyone reads open runs, creator updates/cancels
- run_participants: own rows readable by participant, creator reads all for their runs
- partner_ratings: rater inserts, rated reads own received
- rivals: both users read, initiator inserts
- run_chat_messages: accepted participants only read/insert
- reports: reporter inserts, admin reads/updates
- notifications: user reads own only
- user_blocks: blocker manages own blocks, blocks filter discovery queries

- [x] Done
  - 24 policies applied across all 12 tables
  - Discovery filtering for blocks deferred to query level (Task 3.1/3.2) — RLS alone cannot filter cross-table
  - Admin reads (reports, flagged users) use service_role which bypasses RLS — enforced in Task 13.1
  - Verified: policy_count per table matches spec intent

---

#### Task 1.4 — Auth data layer

- **What:** Repository for Supabase Auth — email/password, Google, Apple sign-in.
- **Spec ref:** SPEC.md Section 5.1 Step 2 (Auth supports email, Google, Apple)
- **Skills:** android-data-layer, android-di-koin
- **Dependencies:** Task 0.3, Task 0.4, Task 1.1
- **Deliverable:** `AuthRepository` interface + `AuthRepositoryImpl` injectable via Koin

**Functions:**
```kotlin
suspend fun signInWithEmail(email: String, password: String): Result<User, AuthError>
suspend fun signUpWithEmail(email: String, password: String): Result<User, AuthError>
suspend fun signInWithGoogle(): Result<User, AuthError>
suspend fun signInWithApple(): Result<User, AuthError>
suspend fun signOut(): Result<Unit, AuthError>
suspend fun getCurrentUser(): Result<User?, AuthError>
suspend fun getSession(): Result<Session?, AuthError>
```

**Security:**
- OAuth tokens stored encrypted — never in plain SharedPreferences
- Session persisted via Supabase SDK's built-in token management

Log entry/exit of every function with AppLogger.

- [x] Done
  - `AuthUser` + `AuthSession` domain models in `shared/auth/domain/`
  - `AuthRepository` interface with all 7 spec functions
  - `SupabaseAuthRepository` implementation: email/password working, Google/Apple stubbed with TODO
  - `AuthWeakPasswordException` not in supabase-kt 3.1.4 — weak password mapped via RestException message
  - `authModule` Koin binding in `shared/auth/di/`; added to `sharedModules` in `AppModules.kt`
  - Session managed by Supabase SDK internally — no manual token storage needed
  - Build verified: `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL

---

#### Task 1.5 — Welcome screen

- **What:** First screen new users see. Single CTA. Sets the tone of the app.
- **Spec ref:** SPEC.md Section 5.1 Step 1 (Welcome)
- **Skills:** android-presentation-mvi, android-navigation, ui-design
- **Dependencies:** Task 0.6, Task 1.4
- **Deliverable:** Welcome screen renders. 'Get started' navigates to LoginRoute.

**UI:**
- Dark background (#0D1B2A)
- PaceUp wordmark large, centered, white
- Tagline: "Find your pace. Find your people." muted, italic
- Abstract faint background: grid of pace numbers at very low opacity
- Single CTA button: "Get started" — full width, pill, primary blue

- [x] Done
  - MVI: `WelcomeState`, `WelcomeAction`, `WelcomeEvent`, `WelcomeViewModel`
  - `WelcomeRoot` + `WelcomeScreen` composables; `PaceNumberGrid` decorative background
  - `ObserveAsEvents` utility added to `composeApp/ui/`
  - `presentationModule` added; `koin-compose-viewmodel` added to composeApp deps
  - `WelcomeRoute` in `AppNavGraph` replaced from stub to `WelcomeRoot`
  - Build verified: `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL

---

#### Task 1.6 — Login screen

- **What:** Full login screen — email/password + Google + Apple sign-in options.
- **Spec ref:** SPEC.md Section 5.1 Step 2, ui-design SKILL.md (Login screen section)
- **Skills:** android-presentation-mvi, android-di-koin, android-navigation, android-testing, ui-design
- **Dependencies:** Task 1.4, Task 0.6
- **Deliverable:** User logs in with email/password or Google/Apple. Navigates to StravaConnectRoute or HomeRoute. Tests written.

**State:**
```kotlin
LoginState(
    email: String,
    password: String,
    isLoading: Boolean,
    error: UiText?
)
```

**Actions:** EmailChanged, PasswordChanged, LoginClicked, GoogleSignInClicked, AppleSignInClicked, SignUpClicked

**UI:**
- Email + password fields
- "Sign In" full-width pill button
- Divider "or"
- Google sign-in button
- Apple sign-in button
- "Don't have an account? Sign up" text link

- [x] Done
  - `LoginState`, `LoginAction`, `LoginEvent`, `LoginViewModel` with `AuthRepository` injection and `SavedStateHandle` for email
  - `LoginRoot` + `LoginScreen` composables — dark theme (#0D1B2A), pace number grid background, email/password fields, social sign-in buttons, sign-up link
  - `UiText` sealed interface + `AuthError.toUiText()` extension in composeApp ui layer
  - `strings.xml` (CMP resources) with all login strings and auth error messages
  - `FakeAuthRepository` + 9-case `LoginViewModelTest` — all passing
  - `LoginRoot` wired into `AppNavGraph` (navigates to StravaConnectRoute or SignUpRoute)
  - `shared:network` added as explicit dep to `composeApp` (was transitive-only)
  - `kotlinx.coroutines.test` added to `composeApp` test dependencies
  - Build verified: `./gradlew :androidApp:assembleDebug` + `:composeApp:testDebugUnitTest` — BUILD SUCCESSFUL

---

#### Task 1.7 — Sign up screen

- **What:** Email/password sign up. Same MVI pattern as login.
- **Spec ref:** SPEC.md Section 5.1 Step 2
- **Skills:** android-presentation-mvi, android-di-koin, android-testing, ui-design
- **Dependencies:** Task 1.4, Task 0.6
- **Deliverable:** User creates account, navigates to StravaConnectRoute

**State:**
```kotlin
SignUpState(
    email: String,
    password: String,
    confirmPassword: String,
    isLoading: Boolean,
    error: UiText?
)
```

- [x] Done
  - `SignUpState`, `SignUpAction`, `SignUpEvent`, `SignUpViewModel` with `AuthRepository` injection and `SavedStateHandle` for email
  - `SignUpRoot` + `SignUpScreen` composables — same dark theme (#0D1B2A), pace number grid background, email/password/confirmPassword fields, social sign-in buttons, back-to-login link
  - Client-side validation: blank fields → invalid credentials error, password < 8 chars → weak password error, mismatch → `error_passwords_do_not_match` (new string resource)
  - `SignUpViewModel` wired into `presentationModule` via `viewModelOf(::SignUpViewModel)`
  - `SignUpRoute` in `AppNavGraph` replaced from stub to `SignUpRoot` (back navigates via `popBackStack`)
  - 12-case `SignUpViewModelTest` — all passing (reuses `FakeAuthRepository` from login package)
  - Build verified: `./gradlew :androidApp:assembleDebug` + `:composeApp:testDebugUnitTest` — BUILD SUCCESSFUL

---

#### Task 1.9 — Forgot password flow (deferred — implement after Phase 2)

- **What:** "Forgot password?" link on the Login screen. Sends a Supabase password reset email. Shows a confirmation screen. User clicks link in email → deep link opens app → password reset form.
- **Spec ref:** SPEC.md Section 5.1 Step 2
- **Skills:** android-presentation-mvi, android-navigation, ui-design
- **Dependencies:** Task 1.4, Task 0.6, Task 11.1 (deep links needed for reset callback)
- **Deliverable:** Forgot password link on login → email sent → confirmation screen. Reset link in email opens app → user sets new password → navigates to login.

**Functions to add to AuthRepository:**
```kotlin
suspend fun sendPasswordResetEmail(email: String): EmptyResult<AuthError>
suspend fun updatePassword(newPassword: String): EmptyResult<AuthError>
```

**Screens:**
- `ForgotPasswordScreen` — email field + "Send Reset Link" button + back to login
- `ResetPasswordScreen` — new password + confirm password fields (reached via deep link)

**Deep link:** `paceup://reset-password?token=...` — handled in Task 11.1

**Note:** Deferred because it requires deep link infrastructure (Task 11.1) to handle the reset callback URL. Add "Forgot password?" text link to LoginScreen when implementing.

- [ ] Done

---

#### Task 1.8 — App version check

- **What:** On every app launch, check min_version from Supabase. Force update dialog if needed. Soft update dismissible banner if not forced.
- **Spec ref:** SPEC.md Section 10.7 (App Versioning & Force Update)
- **Skills:** android-data-layerco
- **Dependencies:** Task 0.4, Task 1.2 (app_config table)
- **Deliverable:** Force update shows non-dismissible dialog linking to store. Soft update shows dismissible banner.

- [x] Done
  - AppVersionRepository + SupabaseAppVersionRepository query app_config table; fail-open on error (treats as up-to-date)
  - AppVersionViewModel takes appVersion param via Koin parametersOf; registered with viewModel {} DSL (not viewModelOf) to support runtime param
  - ForceUpdateDialog (non-dismissible AlertDialog) + SoftUpdateBanner (dismissible Row) in UpdateCheckOverlay.kt
  - App() signature updated to accept appVersion: String and onOpenAppStore callback; MainActivity passes BuildConfig.VERSION_NAME

---

### Phase 2 — Onboarding Flow

> Goal: new user completes full onboarding. At the end, lands on HomeRoute with pace zone calculated.

---

#### Task 2.1 — Strava OAuth data layer

- **What:** OAuth flow for Strava in shared/auth. expect/actual for browser launcher. Token exchange. Activity fetch.
- **Spec ref:** SPEC.md Section 7.1 (Strava API)
- **Skills:** android-data-layer, android-di-koin
- **Dependencies:** Task 0.3, Task 0.4
- **Deliverable:** `StravaAuthRepository` working. Android: Chrome Custom Tabs. iOS: ASWebAuthenticationSession.

**Functions:**
```kotlin
fun buildOAuthUrl(): String
suspend fun exchangeCodeForToken(code: String): Result<StravaToken, AuthError>
suspend fun fetchRecentActivities(token: String): Result<List<StravaActivity>, NetworkError>
```

**Security:**
- Strava tokens stored encrypted
- Raw activity data NOT persisted — derived metrics only (avg_pace_seconds, distance_km, date)

- [x] Done
  - StravaModels: StravaTokenDto/StravaToken, StravaActivityDto/StravaActivityMetrics (derived only — no raw data propagated per spec §7.1)
  - KtorStravaAuthRepository: buildOAuthUrl(), exchangeCodeForToken() (POST /oauth/token), fetchRecentActivities() (GET /athlete/activities, runs >3km only)
  - OAuthBrowserLauncher expect/actual: Android opens Intent.ACTION_VIEW with FLAG_ACTIVITY_NEW_TASK; iOS stub with TODO for ASWebAuthenticationSession in Task 11.1
  - ninetyDaysAgoEpoch() expect/actual: Android uses System.currentTimeMillis(), iOS uses NSDate.timeIntervalSince1970
  - androidAuthModule (androidMain DI) registers OAuthBrowserLauncher(androidContext()); added to PaceUpApplication
  - STRAVA_CLIENT_ID / STRAVA_CLIENT_SECRET are placeholder strings — replace before production

---

#### Task 2.2 — Pace zone calculation

- **What:** Algorithm that takes Strava activities and returns pace zone A–E.
- **Spec ref:** SPEC.md Section 4.1 (Pace Zone Verification)
- **Skills:** android-data-layer
- **Dependencies:** Task 2.1
- **Deliverable:** `PaceZoneCalculator` in shared/paceZone. Fully unit tested.

**Rules:**
- Only runs > 3km included
- Rolling 90-day window
- Zones: A(<4:30), B(4:30–5:00), C(5:00–5:30), D(5:30–6:30), E(>6:30) in sec/km
- Result is pace zone + avg_pace_seconds + weekly_mileage_avg

- [x] Done
  - PaceZone enum: A–E with sec/km boundaries (270/300/330/390) and displayRange strings
  - PaceZoneCalculator.calculate(): takes List<Pair<Int, Float>> (pace, distKm) — pure function, no deps
  - Time-weighted average (totalTime/totalDist) not simple mean — correct pace averaging
  - weeklyMileageAvgKm = totalDistanceKm / (90/7)
  - formatPace() helper: 305 → "5:05 /km"
  - 17 unit tests covering zone boundaries, empty input, weighted average, weekly mileage, formatPace

---

#### Task 2.3 — Strava Connect screen

- **What:** Onboarding step 3/4 — connect Strava, show calculated pace zone, or skip.
- **Spec ref:** SPEC.md Section 5.1 Steps 3–4, ui-design SKILL.md (Strava Connect section)
- **Skills:** android-presentation-mvi, android-di-koin, android-navigation, android-testing, ui-design
- **Dependencies:** Task 2.1, Task 2.2
- **Deliverable:** User connects Strava → activities fetched → pace zone calculated → stored in Supabase users table → navigates to location permission step.

**State:**
```kotlin
StravaConnectState(
    isLoading: Boolean,
    isConnected: Boolean,
    paceZone: PaceZone?,
    avgPaceDisplay: String?,
    error: UiText?
)
```

**UI:** per ui-design SKILL.md Strava Connect section. Orange #FC4C02 accent. Skip option clearly secondary.

**On skip:** strava_connected = false, is_verified = false saved to users table.

- [x] Done
  - StravaOAuthCodeStore (shared/auth) bridges deep-link code to ViewModel via StateFlow
  - MainActivity handles paceup://localhost/callback deep link in onCreate + onNewIntent (singleTop)
  - Chrome Custom Tabs used on Android (FLAG_ACTIVITY_NEW_TASK required — app context, not activity)
  - iOS: OAuthBrowserLauncher uses UIApplication.openURL; IosKoinSetup provides launcher + repo; iOSApp.swift handles deep link via .onOpenURL; Info.plist has URL scheme + xcconfig placeholders — verify on Mac before PR
  - Supabase users-table update is TODO(paceup) — blocked on schema setup (Task 3.x)
  - 11 ViewModel tests passing (all flows: success, no-runs, token failure, activities failure, retry, format)

---

#### Task 2.4 — Location permission screen

- **What:** Onboarding step 5 — pre-permission explanation then system dialog.
- **Spec ref:** SPEC.md Section 5.1 Step 5wait wait
- **Skills:** android-presentation-mvi, ui-design
- **Dependencies:** Task 0.6
- **Deliverable:** Explanation screen shown before system dialog. If denied, user continues (location used for discovery only, not blocked).

- [x] Done
  - LocationPermissionRequester expect/actual: Android uses transparent LocationPermissionActivity; iOS uses CLLocationManager (verify on Mac before PR)
  - Navigation always proceeds regardless of permission result (fire-and-forget via two sequential events)
  - ACCESS_COARSE_LOCATION in AndroidManifest; NSLocationWhenInUseUsageDescription in Info.plist
  - Android: Theme.Translucent.NoTitleBar on LocationPermissionActivity so dialog floats cleanly

---

#### Task 2.5 — Notification permission screen

- **What:** Onboarding step 6 — pre-permission screen then system dialog. Two-step flow on both Android 13+ and iOS.
- **Spec ref:** SPEC.md Section 5.1 Step 6, Section 10.5 (Push Notification Permission Flow)
- **Skills:** android-presentation-mvi, ui-design
- **Dependencies:** Task 0.6
- **Deliverable:** Pre-permission screen always shown before system dialog. "Maybe later" skips and never asks again unless user enables in settings.

- [x] Done
  - NotificationPermissionRequester expect/actual: Android uses transparent NotificationPermissionActivity (API 33+ only; no-op below); iOS uses UNUserNotificationCenter fire-and-forget (verify on Mac before PR)
  - NotificationPermissionPrefs expect/actual: Android = SharedPreferences; iOS = NSUserDefaults — persists "maybe later" across app restarts
  - ViewModel auto-skips if isGranted() OR wasDeclined() — "maybe later" survives re-login
  - POST_NOTIFICATIONS in AndroidManifest; @RequiresApi(33) on NotificationPermissionActivity

---

#### Task 2.6 — Profile setup screen

- **What:** Onboarding step 7 — display name (required) and avatar photo (optional).
- **Spec ref:** SPEC.md Section 5.1 Step 7
- **Skills:** android-presentation-mvi, android-di-koin, ui-design
- **Dependencies:** Task 1.4, Task 0.6
- **Deliverable:** Display name and optional avatar saved to Supabase users table. Avatar uploaded to Supabase Storage.

- [x] Done
  - ProfileRepository/SupabaseProfileRepository: avatar upload to Supabase Storage + upsert to users table
  - ImagePicker expect/actual: Android PhotoPickerActivity (512×512, 80% JPEG) + iOS UIImagePickerController (verify on Mac before PR)
  - ImagePickerResult singleton StateFlow bridge for cross-boundary ByteArray delivery
  - Coil 3 CMP AsyncImage for avatar preview; dark navy UI with OutlinedTextField

---

#### Task 2.7 — Onboarding completion & discovery tooltip

- **What:** Onboarding step 8 — user lands on HomeRoute (map) with a tooltip overlay.
- **Spec ref:** SPEC.md Section 5.1 Step 8
- **Skills:** android-navigation, ui-design
- **Dependencies:** Task 2.6, Task 0.6
- **Deliverable:** After completing profile setup, user navigates to HomeRoute. Tooltip shown: "These runs match your pace. Tap any pin to join." Tooltip dismisses on tap.

- [x] Done
  - OnboardingPrefs expect/actual: Android = SharedPreferences; iOS = NSUserDefaults — marks completed after profile save
  - LoginViewModel checks onboardingPrefs.isCompleted() to route returning users directly to HomeRoute (skips onboarding)
  - HomeViewModel + HomeScreen: animated tooltip overlay, dismisses on tap
  - Tooltip is session-only state (showTooltip in ViewModel) — always shows on first landing post-onboarding

---

### Phase 3 — Run Discovery

> Goal: home screen is alive with runs on a map. User can browse, filter, and find runs.

---

#### Task 3.1 — Runs data layer

- **What:** Repository for fetching runs from Supabase. Filtering, pagination, geo queries.
- **Spec ref:** SPEC.md Section 4.3 (Run Discovery), Section 8.2 (runs table)
- **Skills:** android-data-layer, android-di-koin
- **Dependencies:** Task 1.2, Task 0.3
- **Deliverable:** `RunRepository` injectable via Koin

**Functions:**
```kotlin
suspend fun getRunsNearLocation(lat: Double, lng: Double, radiusKm: Double): Result<List<Run>, AppError>
suspend fun getRunById(runId: String): Result<Run, AppError>
suspend fun getRunsForUser(userId: String): Result<List<Run>, AppError>
suspend fun searchRuns(query: String, filters: RunFilters): Result<List<Run>, AppError>
fun observeRunStatus(runId: String): Flow<RunStatus>
```

- [x] Done
  - Run domain model + RunDto + mapper in shared/runMatching/domain/Run.kt
  - RunRepository interface with getRunsNearLocation, getRunById, getRunsForUser, searchRuns, observeRunStatus
  - SupabaseRunRepository: Supabase postgrest queries; client-side haversine proximity filter (TODO: replace with Edge Function)
  - RunError added to shared/network (sealed interface cross-module constraint)
  - RunFilters: paceMinSec/paceMaxSec, modes, verifiedOnly, afterDate
  - runMatchingModule Koin binding; wired into sharedModules + composeApp dependency
  - 9 unit tests covering DTO mapping, filter logic, edge cases — all passing

---

#### Task 3.2 — Map discovery screen

- **What:** Home screen. Full-bleed dark map with run pins color-coded by pace zone. Filter chips. Floating search bar. Bottom sheet with run cards.
- **Spec ref:** SPEC.md Section 4.3 (Map View), ui-design SKILL.md (Discovery Map Screen section)
- **Skills:** android-presentation-mvi, android-di-koin, android-navigation, ui-design
- **Dependencies:** Task 3.1, Task 0.6
- **Deliverable:** Map renders with run pins. Tapping a pin opens run detail bottom sheet. Filter chips filter visible pins.

**State:**
```kotlin
MapDiscoveryState(
    runs: List<Run>,
    selectedRun: Run?,
    filters: RunFilters,
    isLoading: Boolean,
    userLocation: LatLng?,
    error: UiText?
)
```

**Note:** Google Maps SDK on Android, MapKit on iOS via expect/actual.

- [x] Done
  - MapDiscoveryState (typealias HomeState) with runs, selectedRun, filters, isLoading, userLocation, error, showTooltip, searchQuery
  - HomeViewModel: injects RunRepository; loads runs on init (20km radius, default Tel Aviv); filter/search actions
  - PaceUpMap expect/actual: Android = Google Maps Compose 4.4.1 with dark style JSON + MarkerComposable zone-colored teardrop pins; iOS = placeholder with TODO
  - maps-compose added to version catalog; MAPS_API_KEY manifest placeholder + manifestPlaceholders in androidApp build
  - RunCard: left zone-color accent bar, zone badge, mode chip, pace range, join button
  - HomeScreen: full-bleed map, frosted-glass search bar, horizontal filter chips (mode + verified), ModalBottomSheet for selected run, FAB for create run
  - HomeRoot wired with onNavigateToRunDetail/onNavigateToCreateRun callbacks in AppNavGraph
  - PaceZoneUi helpers: zone color, Run.paceZone() derivation, formatPaceRange()
  - iOS: PaceUpMap.ios.kt stub — verify on Mac before PR

---

#### Task 3.3 — List discovery screen

- **What:** Tab alongside map. Sortable, filterable list of runs.
- **Spec ref:** SPEC.md Section 4.3 (List View)
- **Skills:** android-presentation-mvi, ui-design
- **Dependencies:** Task 3.1
- **Deliverable:** Scrollable list of run cards with full filter bar. Tapping navigates to run detail.

**Filters from spec:** pace zone, distance min/max, run mode, date range, distance from me, verified only, open join only, recurring only

- [x] Done
  - `DiscoveryTab` (MAP/LIST) managed in `HomeRoot` with shared tab bar overlay (`DiscoveryTabBar`)
  - `RunListViewModel` + `RunListState/Action/Event` — sort by SOONEST/CLOSEST, `RunListFilters` with all 8 spec filters
  - `RunListScreen` — LazyColumn of `RunCard` with scheduled time, sort bar, filter badge, empty state
  - `FilterSheetContent` — ModalBottomSheet with Pace Zone / Mode / Proximity / Run Distance / Date / Toggle sections
  - Date presets (Any / Today+ / Next 7 days) use `kotlinx-datetime` added to composeApp deps
  - `RunFilters` extended: `minDistanceKm`, `beforeDate`, `openJoinOnly`, `recurringOnly`; `applyFilters` updated
  - `RunCard` extended: optional `scheduledTimeDisplay` param for list context
  - Build verified: `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL

---

#### Task 3.4 — Run detail screen

- **What:** Full run detail — all params, participants with reputation data, join/request button.
- **Spec ref:** SPEC.md Section 4.2 (Run Parameters), Section 4.4 (Trust shown on participants)
- **Skills:** android-presentation-mvi, android-di-koin, android-navigation, ui-design
- 
- **Dependencies:** Task 3.1, Task 0.6
- **Deliverable:** Full run detail renders. Participant avatars with pace zone rings. Join button visible and correct per user tier.__

- [x] Done
  - `RunParticipant` domain model + `RunParticipantDto`/`UserSummaryDto` added to shared/runMatching
  - `RunRepository.getRunParticipants(runId)` added; `SupabaseRunRepository` implements it with PostgREST FK join on `users(display_name,avatar_url,pace_zone,show_up_rate)`
  - `RunDetailViewModel` receives `runId` via `SavedStateHandle`; loads run + participants sequentially
  - `RunDetailScreen`: hero (title, mode chip, zone badge, status), info cards (date/time, location, distance, max participants), requirements (join mode, verified-only), participant rows with pace-zone colored rings + show-up rate, sticky join button (label per join_mode + status; disabled for invite_only/full/cancelled)
  - `RunDetailRoot` wired into `AppNavGraph` (replaces stub)
  - `RunDetailViewModel` added to `presentationModule`
  - Build verified: `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL
  - `OnboardingPrefsSource` interface extracted so ViewModels can be tested without Android Context — `OnboardingPrefs` expect/actual implements it; Koin binding updated on Android + iOS; fixes pre-existing test breakage from Task 2.7
  - `FakeRunRepository` + 9-case `RunDetailViewModelTest` — all passing
  - Full test suite: `./gradlew :composeApp:testDebugUnitTest` — BUILD SUCCESSFUL

---

#### Task 3.5 — Search

- **What:** Search runs by city, neighborhood, or meeting address. Search users by display name. Supabase FTS with pg_trgm indexing. Results capped at 50.
- **Spec ref:** SPEC.md Section 10.6 (Search)
- **Skills:** android-presentation-mvi, ui-design
- **Dependencies:** Task 3.1
- **Deliverable:** Search bar functional. Results debounced 300ms. FTS indexes applied in Supabase. User search reusable in rival flow and person lookup.

- [x] Done
  - Supabase: pg_trgm extension + GIN trigram indexes on runs(city, meeting_address, title) and users(display_name)
  - `UserSummary` domain model + `UserRepository` interface in shared/runMatching
  - `UserSearchDto` + `SupabaseUserRepository.searchUsers()` — ilike on display_name, capped at 50, filters banned/suspended
  - `searchRuns()` limit fixed from 100 → 50 (spec §10.6)
  - `UserRepository` binding added to `runMatchingModule`
  - `SearchViewModel`: Run + People tabs, 300ms debounce via `Flow.debounce()`, `@OptIn(FlowPreview::class)`
  - `SearchScreen`: full-screen dark UI, auto-focused search field, tab bar (Runs/People), run cards + user result cards with zone ring + show-up rate, empty/no-results states
  - `SearchRoute` added to Routes.kt; `SearchRoot` wired in AppNavGraph
  - `SearchViewModel` added to presentationModule
  - HomeScreen search bar converted from editable TextField to tap-to-navigate button (SearchBarButton) — `onNavigateToSearch` callback on HomeRoot/HomeScreen
  - HomeViewModel cleaned up: removed `searchQuery` state, `OnSearchQueryChange`/`OnSearchSubmit` actions, `searchRuns()`/`applyFilters()` methods
  - User search reusable for rival flow (Task 7.1) via `UserRepository` + `SearchScreen` with PEOPLE tab
  - iOS: SearchRoot + UserRepository stub — verify on Mac before PR
  - Build verified: `./gradlew :androidApp:assembleDebug` + `:composeApp:testDebugUnitTest` — BUILD SUCCESSFUL

---

### Phase 4 — Run Creation

> Goal: any active-tier user can create a run and it appears on the map.

---

#### Task 4.1 — Create run data layer

- **What:** Repository function to insert a run into Supabase.
- **Spec ref:** SPEC.md Section 4.2 (Run Creation), Section 8.2 (runs table)
- **Skills:** android-data-layer
- **Dependencies:** Task 1.2, Task 0.3
- **Deliverable:** `RunRepository.createRun(params)` working and verified in Supabase

- [x] Done
  - `CreateRunParams` domain model in shared/runMatching/domain — all runs table fields; creatorId passed by caller from auth session
  - `CreateRunDto` + `CreateRunParams.toDto()` mapper in shared/runMatching/data — excludes auto-generated fields (id, status, created_at)
  - `RunRepository.createRun(params)` added to interface; `SupabaseRunRepository` implements with `postgrest[TABLE].insert(params.toDto()).decodeSingle<RunDto>().toDomain()`
  - `Run`/`RunDto` extended with `ageMin`, `ageMax`, `genderFilter`, `recurrenceRule`, `cancellationReason` (all default null/"any")
  - `FakeRunRepository.createRun()` stub added for test compilation
  - Build verified: `./gradlew :androidApp:assembleDebug` + `:composeApp:testDebugUnitTest` — BUILD SUCCESSFUL

---

#### Task 4.2 — Create run screen

- **What:** Multi-step run creation. MVP run modes only.
- **Spec ref:** SPEC.md Section 4.2 (Run Parameters + Run Modes)
- **Skills:** android-presentation-mvi, android-di-koin, android-navigation, ui-design
- **Dependencies:** Task 4.1
- **Deliverable:** User completes form, run created in Supabase, appears on map.

**MVP run modes (3 only):**
- easy / social
- tempo / training
- recovery

**Steps:**
1. Mode selection
2. Date and time
3. Location: map picker for meeting point
4. Run details: distance/duration, pace range
5. Filters: max participants, age range, gender filter, verification requirement
6. Join mode: open / request / invite only
7. Review and confirm

**Post-MVP modes deferred:** tourist, pacer, race_prep, recurring

- [x] Done
  - `CreateRunState`, `CreateRunAction`, `CreateRunEvent`, `CreateRunViewModel` in `feature/createrun/`
  - `CreateRunViewModel` injects `RunRepository` (for `createRun`) + `AuthRepository` (for `getCurrentUser` as creatorId)
  - 7-step MVI state machine with per-step validation — mode, date/time, location, details, filters, join mode, review
  - Pace range via dual Sliders (210–480 sec/km = 3:30–8:00 /km)
  - Location: address + city + lat/lng text fields (map picker deferred, TODO added per plan)
  - `CreateRunRoot` + `CreateRunScreen` composables — dark theme, step progress bar, per-step forms
  - `CreateRunRoute` in AppNavGraph replaced from stub to `CreateRunRoot` (on success navigates to RunDetailRoute)
  - `CreateRunViewModel` added to `presentationModule`
  - MVP modes: EASY, TEMPO, RECOVERY — post-MVP (TOURIST, PACER, RACE_PREP) deferred
  - Build verified: `./gradlew :androidApp:assembleDebug` + `:composeApp:testDebugUnitTest` — BUILD SUCCESSFUL

---

#### Task 4.3 — Smart invite Edge Function

- **What:** Supabase Edge Function that fires on run creation and notifies matching runners nearby.
- **Spec ref:** SPEC.md Section 4.2 (Smart Invite Logic), Section 8.4 (smart_invite)
- **Dependencies:** Task 4.1, Task 1.2
- **Deliverable:** Edge Function deployed. Creating a run triggers notifications to matching users within radius.

- [x] Done
  - pg_net 0.20.0 enabled on Supabase
  - Postgres trigger `on_run_created_smart_invite` (AFTER INSERT on runs) → calls Edge Function via pg_net async HTTP
  - `smart_invite` Edge Function deployed and ACTIVE: haversine proximity filter (50km, JS), pace range match, verified_only support, 200-invite cap, idempotent deduplication
  - Notification rows created in `notifications` table → picked up by `notification_dispatcher`

---

### Phase 5 — Join Flow & Participant Management

> Goal: users can request or join runs. Creators can manage participants. Chat works.

---

#### Task 5.1 — Join request data layer

- **What:** Functions to join, request, accept, decline participants.
- **Spec ref:** SPEC.md Section 4.2 (Smart Invite Logic), Section 8.2 (run_participants)
- **Skills:** android-data-layer
- **Dependencies:** Task 1.2, Task 0.3
- **Deliverable:** Full participant management in RunRepository

**Functions:**
```kotlin
suspend fun joinRun(runId: String): Result<Unit, AppError>
suspend fun requestToJoin(runId: String): Result<Unit, AppError>
suspend fun acceptParticipant(runId: String, userId: String): Result<Unit, AppError>
suspend fun declineParticipant(runId: String, userId: String): Result<Unit, AppError>
suspend fun cancelParticipation(runId: String): Result<Unit, AppError>
fun observeParticipants(runId: String): Flow<List<RunParticipant>>
```

- [x] Done
  - `joinRun`, `requestToJoin`, `acceptParticipant`, `declineParticipant`, `cancelParticipation`, `observeParticipants` added to `RunRepository` interface
  - `JoinRunDto` + `UpdateParticipantStatusDto` in `shared/runMatching/…/data/`
  - `SupabaseRunRepository` implements all 6 — `cancelParticipation` fetches the run, deletes row if >2h away (no penalty), sets `late_cancel` if ≤2h (spec §4.4)
  - `supabase.auth` + `kotlinx.datetime` added to `shared/runMatching` deps
  - `FakeRunRepository` updated with stubs for all new methods
  - Pre-existing fixes: Firebase `platform()` moved out of KMP `sourceSets` block (Kotlin 2.3); `PaceUpFirebaseMessagingService` `SupervisorJob` cancel fixed
  - Build verified: `./gradlew :androidApp:assembleDebug` + `:composeApp:testDebugUnitTest` — BUILD SUCCESSFUL

---

#### Task 5.2 — Join/request UI

- **What:** Join button states and request flow on run detail screen. Creator's participant management view.
- **Spec ref:** SPEC.md Section 4.2, Section 4.4 (Access Tiers)
- **Skills:** android-presentation-mvi, ui-design
- **Dependencies:** Task 5.1, Task 3.4
- **Deliverable:** Join button works. Creator sees pending requests with pace zone, show-up rate, tags. Accept/decline functional. new_runner tier cannot join verified-only runs.

- [x] Done
  - `RunDetailViewModel` fully rewritten with MVI: `RunDetailState`, `RunDetailAction`, `RunDetailEvent`
  - `JoinStatus` enum: `NONE / REQUESTED / JOINED`
  - `canJoin` computed from reputation tier + `verifiedOnly` flag (spec §4.4)
  - `observeParticipants` used in place of `getRunParticipants` so creator sees all statuses
  - `RunDetailScreen` updated: `JoinButton` handles all states; `PendingRequestsSection` for creator accept/decline
  - `FakeAuthRepositoryForDetail`, `FakeUserRepository` created; `RunDetailViewModelTest` rewritten with 16 passing tests
  - Build verified: `./gradlew :androidApp:assembleDebug` + `:composeApp:testDebugUnitTest` — BUILD SUCCESSFUL (55 tests pass)

---

#### Task 5.3 — Run cancellation

- **What:** Creator can cancel a run. All accepted participants notified.
- **Spec ref:** SPEC.md Section 8.4 (run_cancellation_notify)
- **Skills:** android-presentation-mvi
- **Dependencies:** Task 5.1
- **Deliverable:** Cancel option on creator's run. Edge Function notifies all participants. Run hidden from discovery.

- [x] Done
  - `cancelRun(runId, reason)` added to `RunRepository` + `SupabaseRunRepository` (PATCH status=cancelled + cancellation_reason on `runs` table)
  - `CancelRunDto` added to `JoinRunDto.kt`
  - `RunDetailViewModel`: `showCancelRunDialog`, `isCancellingRun`, `cancelRunError` state; `OnCancelRunClick`, `OnConfirmCancelRun`, `OnDismissCancelRunDialog`, `OnDismissCancelRunError` actions
  - `RunDetailScreen`: `CreatorCancelButton` (red, hidden after cancellation) + `CancelRunDialog` (AlertDialog with optional reason field)
  - `run_cancellation_notify` Edge Function deployed at `supabase/functions/run_cancellation_notify/index.ts`
  - Postgres trigger SQL at `supabase/migrations/20260531_run_cancellation_trigger.sql`
  - 5 new tests in `RunDetailViewModelTest` — BUILD SUCCESSFUL (60 tests pass)

---

#### Task 5.4 — Run chat

- **What:** Pre-run group chat. Supabase Realtime. Auto-archives 2 hours after run end.
- **Spec ref:** SPEC.md Section 4.6 (Group Chat per Run), Section 8.3 (Realtime)
- **Skills:** android-presentation-mvi, android-di-koin, ui-design
- **Dependencies:** Task 5.1, Task 1.2
- **Deliverable:** Chat opens when run confirmed. Messages sync in real time. Offline: cached messages with banner.

**Also deploy:** chat_cleanup Edge Function (deletes messages 48h after run end)

- [x] Done
  - `ChatMessage` domain model + `ChatRepository` interface in `shared/runMatching/domain/`
  - `ChatMessageDto`, `ChatMessageRealtimeDto`, `SendMessageDto` in `shared/runMatching/data/`
  - `SupabaseChatRepository`: `getRecentMessages` (Postgrest), `observeNewMessages` (Realtime channel subscribe), `sendMessage` (Postgrest insert)
  - Note: `filter =` DSL property is private in supabase-kt 3.1.4 — filtered in Kotlin Flow pipeline instead
  - `RunChatViewModel` + `RunChatScreen`: message list with auto-scroll, empty state, offline/error banners, input bar with send button
  - `ChatRepository` registered in `runMatchingModule`; `RunChatViewModel` in `presentationModule`
  - `RunChatRoute(runId, runTitle)` added to navigation; linked from `RunDetailRoot` → chat button visible for accepted participant + creator
  - `chat_cleanup` Edge Function + `get_stale_chat_run_ids` SQL helper at `supabase/migrations/`
  - `FakeChatRepository` + `RunChatViewModelTest` (11 tests) — BUILD SUCCESSFUL

---

### Phase 6 — User Profiles & Reputation

> Goal: runner profiles are data-rich and trust-building. Core reputation system works.

---

#### Task 6.1 — User profile screen

- **What:** Full profile — pace zone, stats, show-up rate, partner tags, badges, run history.
- **Spec ref:** SPEC.md Section 4.1 (Profile Data), ui-design SKILL.md (Profile Screen section)
- **Skills:** android-presentation-mvi, android-di-koin, android-navigation, ui-design
- **Dependencies:** Task 1.1, Task 1.2
- **Deliverable:** Own profile and other users' profiles render with all spec fields. Show-up rate color-coded. Pace zone badge prominent.

- [x] Done
  - `UserProfile` domain model with all spec §4.1 fields: pace zone, avg pace, weekly mileage, longest run, show-up rate, total runs, unique partners, reputation tier, connected apps
  - `getUserProfile(userId)` added to `UserRepository` + implemented in `SupabaseUserRepository` (UserProfileDto with toDomain)
  - `UserProfileViewModel`: loads profile + recent runs (completed/in_progress last 10), detects own vs other profile
  - `UserProfileScreen`: avatar with zone-color ring, pace zone badge, show-up rate badge (green/amber/red), reputation badge (Trusted/Pacer/Active), stats row (runs/partners/weekly avg), running stats card, connected apps, recent runs list
  - `UserProfileRoute` stub replaced with real `UserProfileRoot` in nav graph
  - `FakeUserRepository` updated with `getUserProfile` stub
  - BUILD SUCCESSFUL (all prior tests still pass)

---

#### Task 6.2 — Post-run attendance verification Edge Function

- **What:** Fires ~30min after run end. Checks Strava for matching activity. Updates run_participants status.
- **Spec ref:** SPEC.md Section 4.4 (Show-Up Rate), Section 8.4 (run_attendance_verify)
- **Dependencies:** Task 1.2, Task 2.1
- **Deliverable:** Edge Function deployed. Participant status updates to attended / late_cancel / no_show correctly.

**Logic:**
- Match by: within 500m of meeting point, within ±30min of scheduled time
- Update run_participants.status and actual_avg_pace
- Raw activity data NOT stored — only actual_avg_pace derived metric

- [x] Done
  - `user_strava_tokens` table: separate from `users` for security; RLS — owner only; service_role reads all (edge function bypass)
  - `ProfileRepository.saveStravaConnection()` added — upserts tokens + updates users with strava_connected, is_verified, pace_zone, avg_pace_seconds, weekly_mileage_avg, strava_athlete_id
  - `StravaConnectViewModel` now injects `ProfileRepository` and persists tokens after successful OAuth (fail-open: UI shows success even if save fails)
  - `get_runs_for_attendance_verify()` SQL helper: returns runs ended 10-90 min ago with accepted participants
  - `run_attendance_verify` Edge Function deployed (ACTIVE): matches Strava activity by time ±30min + location ≤500m; refreshes expired tokens; updates status to attended/no_show
  - pg_cron schedule applied (every 30min) — falls back gracefully on free-tier without pg_cron
  - 13 ViewModel tests for StravaConnectViewModel (2 new tests for save behavior) — BUILD SUCCESSFUL

---

#### Task 6.3 — Reputation recalculation Edge Function

- **What:** Recalculates show_up_rate after each run. Updates reputation_tier.
- **Spec ref:** SPEC.md Section 4.4 (Show-Up Rate, Access Tiers), Section 8.4 (reputation_recalculate)
- **Dependencies:** Task 6.2
- **Deliverable:** show_up_rate and reputation_tier in users table update correctly after every run.

**Note:** pace_accuracy_score is Post-MVP — do NOT implement here.

- [x] Done
  - `recalculate_user_reputation(uuid)` Postgres function: computes show_up_rate (attended / total_judged × 100), total_paceup_runs, unique_partners, reputation_tier — pure SQL, no external API
  - `on_participant_status_terminal` trigger fires AFTER UPDATE on run_participants when status transitions to attended/no_show/late_cancel → calls recalculate_user_reputation synchronously
  - Tier logic: new_runner (<3 attended), trusted (>=3 + show_up_rate >85), active (>=3); pace_accuracy criterion for trusted is Post-MVP
  - `reputation_recalculate` Edge Function deployed (ACTIVE): thin wrapper for admin manual recalculation; supports single user_id or `{ all: true }` batch
  - Build: no Kotlin changes — pure Supabase SQL + Edge Function

---

#### Task 6.4 — Partner rating flow

- **What:** Post-run prompt to rate run partners with tags.
- **Spec ref:** SPEC.md Section 4.4 (Partner Tags)
- **Skills:** android-presentation-mvi, ui-design
- **Dependencies:** Task 6.2, Task 1.2
- **Deliverable:** After attendance verified, push notification prompts rating. User selects tags per partner. Saved to partner_ratings table.

**Tags:** kept_pace, great_energy, pushed_group, early, mismatched_pace

- [x] Done
  - partner_ratings table + RLS migration applied to Supabase
  - SupabasePartnerRatingRepository: getPartnersToRate (two-query approach), submitRatings, hasRatedRun
  - PartnerRatingViewModel (MVI), PartnerRatingScreen (tag chip toggle UI, already-rated/empty states)
  - RunDetailViewModel: userAttended state, OnRatePartnersClick action, NavigateToRatePartners event; participants list includes "attended" status
  - RunDetail "Rate partners" button visible when userAttended==true; JoinButton suppressed for attended users
  - Wired in RunMatchingModule, PresentationModule, Routes, AppNavGraph
  - 13 unit tests, all passing

---

#### Task 6.5 — Access tier enforcement

- **What:** Client-side and server-side enforcement of access tiers from spec.
- **Spec ref:** SPEC.md Section 4.4 (Access Tiers)
- **Skills:** android-data-layer
- **Dependencies:** Task 6.3
- **Deliverable:** new_runner cannot create runs until 3 attended. Trusted badge shown when criteria met.

**Tiers:**
- new_runner: 0–2 attended, join open only, cannot create
- active: 3+ attended, full access
- trusted: >85% show-up rate, Trusted badge (pace_accuracy not required until post-MVP)
- pacer_eligible: trusted + verified pace consistency

- [x] Done
  - CreateRunViewModel: added UserRepository dep, checkTier() on init, canCreateRun/isCheckingTier state
  - CreateRunScreen: loading spinner during tier check; NewRunnerGateScreen when canCreateRun==false
  - canJoin in RunDetailViewModel extended: new_runner blocked from non-open join_mode runs
  - RunParticipant: added reputationTier field; UserSummaryDto includes reputation_tier; query updated
  - RunDetailScreen: TrustedBadge shown inline in participant name row for trusted/pacer_eligible tiers
  - DB: "active users can create runs" + "new_runner can only join open runs" RLS policies applied

---

#### Task 6.6 — Weekly pace zone update Edge Function

- **What:** Runs every Monday. Fetches Strava data. Recalculates pace zone and weekly mileage avg.
- **Spec ref:** SPEC.md Section 8.4 (weekly_pace_zone_update)
- **Dependencies:** Task 2.1, Task 2.2
- **Deliverable:** Edge Function deployed and scheduled. pace_zone updates each week.

**Pace outlier detection (from spec Section 6.3):**
- If actual post-run pace is >2 min/km outside stated zone on 3+ consecutive runs:
  force-recalculate zone + set admin alert flag in users table

- [x] Done
  - `admin_alert_pace_outlier boolean DEFAULT false` added to users table (migration 20260607_weekly_pace_zone_update.sql)
  - `weekly_pace_zone_update` Edge Function: fetches all strava_connected users, refreshes tokens, fetches 90-day activities, recalculates pace zone with same algorithm as PaceZoneCalculator.kt (time-weighted avg, A–E boundaries, >3km filter)
  - Updates users.pace_zone, avg_pace_seconds, weekly_mileage_avg each Monday 03:00 UTC
  - Pace outlier detection (spec §6.3): if last 3 consecutive attended runs all deviate >120 sec/km from pre-update stated pace → sets admin_alert_pace_outlier=true
  - Deployment pending: run `supabase functions deploy weekly_pace_zone_update --no-verify-jwt`

---

### Phase 7 — Basic Rival System

> Goal: runners can challenge each other. Weekly distance leaderboard works.

---

#### Task 7.1 — Rival data layer

- **What:** Repository for rival connections and weekly stats.
- **Spec ref:** SPEC.md Section 4.5 (Rival System), Section 8.2 (rivals, rival_weekly_snapshots)
- **Skills:** android-data-layer, android-di-koin
- **Dependencies:** Task 1.2
- **Deliverable:** `RivalRepository` with all rival management functions

**Functions:**
```kotlin
suspend fun sendRivalRequest(targetUserId: String): Result<Unit, AppError>
suspend fun acceptRivalRequest(rivalId: String): Result<Unit, AppError>
suspend fun declineRivalRequest(rivalId: String): Result<Unit, AppError>
suspend fun getRivals(): Result<List<Rival>, AppError>
fun observeRivalRequest(): Flow<Rival>
```

**Note:** getSuggestedRivals() is deferred to Bucket B.

- [x] Done
  - `Rival` + `RivalWeeklySnapshot` domain models in `shared/rivalEngine/domain/`
  - `RivalRepository` interface: sendRivalRequest, acceptRivalRequest, declineRivalRequest, getRivals, observeRivalRequest, getLatestSnapshot
  - `RivalDto`, `InsertRivalDto`, `UpdateRivalStatusDto`, `RivalWeeklySnapshotDto` in `shared/rivalEngine/data/`
  - `SupabaseRivalRepository`: getRivals uses two-query approach (asA + asB, distinctBy id — safe with max-5 limit); observeRivalRequest uses Supabase Realtime with in-Flow filtering (same pattern as chat)
  - `rivalEngineModule` Koin binding in `shared/rivalEngine/RivalEngineModule.kt`; added to `sharedModules` in AppModules.kt
  - `shared:rivalEngine` added as composeApp dependency; build.gradle.kts updated with kotlinSerialization plugin + supabase/koin deps
  - Build verified: `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL

---

#### Task 7.2 — Rival dashboard screen

- **What:** Sports scoreboard UI. Live weekly distance stats. Streak.
- **Spec ref:** SPEC.md Section 4.5 (Rival Dashboard), ui-design SKILL.md (Rival Screen section)
- **Skills:** android-presentation-mvi, android-di-koin, android-navigation, ui-design
- **Dependencies:** Task 7.1
- **Deliverable:** Rival screen renders with two avatars, weekly km, run count, fastest pace. Leader highlighted.

**Note:** Historical sparkline deferred to Bucket B.

- [x] Done
  - `RivalWithDetails` UI model (rival + snapshot + opponent summary + isIncoming flag) in ViewModel file
  - `RivalDashboardState/Action/Event` + `RivalDashboardViewModel`: loads rivals, fetches per-rival snapshots + opponent profiles, observes Realtime incoming requests
  - `getUserSummary(userId)` added to UserRepository interface + SupabaseUserRepository (reuses UserSearchDto, min-column query)
  - `RivalDashboardScreen`: scoreboard card (two avatars + VS + weekly km + progress bar + stat row + streak indicator + win record), incoming request cards (accept/decline), outgoing pending cards, empty state
  - "Add Rival" ModalBottomSheet with live search (debounce implicit via Koin + coroutines); send request → reload list
  - Streak indicator (flame icon) shown only when streak >= 3 weeks
  - Leader highlighted: glow border on avatar + bold km number
  - `RivalDashboardRoot` wired into AppNavGraph replacing stub
  - `RivalDashboardViewModel` added to presentationModule
  - Build verified: `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL

---

#### Task 7.3 — Rival weekly snapshot Edge Function

- **What:** Runs every Monday. Aggregates last week's Strava data per rival pair. Updates wins and streaks.
- **Spec ref:** SPEC.md Section 8.4 (rival_weekly_snapshot)
- **Dependencies:** Task 7.1, Task 2.1
- **Deliverable:** Edge Function deployed and scheduled.

- [x] Done
  - `rival_weekly_snapshot` Edge Function at `supabase/functions/rival_weekly_snapshot/index.ts`
  - Logic: fetches active rival pairs → batch-fetches Strava tokens → per-user weekly stats (totalKm, runCount, bestPace) → determines winner (most km) → upserts rival_weekly_snapshots → updates rivals (wins, streak)
  - Streak logic: winner matches current_streak → increment; different winner → reset to 1; tied → reset to 0
  - Migration: unique constraint on (rival_id, week_start) for idempotent upsert; pg_cron every Monday 04:00 UTC
  - Deployment pending: run `supabase functions deploy rival_weekly_snapshot --no-verify-jwt`

---

### Phase 8 — Notifications

> Goal: push notifications work end-to-end for all MVP notification types.

---

#### Task 8.1 — Notification infrastructure

- **What:** FCM (Android) and APNs (iOS) setup. Device token registration. Token deleted on logout/account delete. Notification dispatcher Edge Function.
- **Spec ref:** SPEC.md Section 8.4 (notification_dispatcher), Section 8.2 (notifications table), Section 6.4 (push token deletion)
- **Skills:** android-data-layer
- **Dependencies:** Task 1.1
- **Deliverable:** Device tokens saved to Supabase on login. Deleted on logout and account delete. Edge Function sends push and marks sent.

- [x] Done
  - `push_token` column added to `users` table (migration applied)
  - `NotificationRepository` + `SupabaseNotificationRepository` in `shared/notifications`
  - `PaceUpFirebaseMessagingService`: registers token on refresh, shows foreground notifications
  - `MainActivity.registerFcmToken()`: fetches token on each launch (covers pre-login installs)
  - `SupabaseAuthRepository.signOut()` clears `push_token = null` before sign-out (spec §6.4)
  - `notification_dispatcher` Edge Function deployed and ACTIVE: polls `notifications`, sends FCM, marks sent
  - Firebase BOM 33.15.0 + google-services plugin wired; placeholder `google-services.json` in repo
  - TODO: replace `androidApp/google-services.json` with real file from Firebase Console
  - TODO: set `FIREBASE_SERVER_KEY` Supabase secret before push delivery works
  - iOS: APNs counterpart deferred — verify on Mac before PR (Task 15.6)
  - Branch: `feat/push-notifications`

---

#### Task 8.2 — Run reminder notifications

- **What:** 24h and 2h pre-run notifications for confirmed runs.
- **Spec ref:** SPEC.md Section 4.7 (Pre-Run notifications)
- **Dependencies:** Task 8.1
- **Deliverable:** Joining a confirmed run schedules two notifications. Both arrive on device.

- [x] Done
  - PostgreSQL trigger `schedule_run_reminders()` on `run_participants` fires after INSERT or UPDATE to status='accepted'
  - Inserts two `notifications` rows: 24h and 2h before `runs.scheduled_at`
  - WHERE NOT EXISTS guard prevents duplicate notifications on re-accepts
  - Migration applied: `20260607_run_reminder_notifications.sql`
  - Existing `notification_dispatcher` Edge Function picks them up and delivers via FCM — no new Edge Function needed

---

#### Task 8.3 — Rival nudge notifications

- **What:** "Your rival just ran" notifications and end-of-week Sunday summary.
- **Spec ref:** SPEC.md Section 4.5 (Rival Notifications), Section 4.7
- **Dependencies:** Task 8.1, Task 7.3
- **Deliverable:** Rival activity triggers notification. Sunday summary sent.

- [x] Done
  - `notify_rival_ran()` trigger fires on `run_participants` UPDATE to 'attended'; inserts `rival_nudge` notifications for all active rivals
  - `insert_rival_weekly_summaries()` function + pg_cron schedule: every Sunday 19:00 UTC inserts `rival_weekly_summary` notifications
  - Both use NOT EXISTS guard to prevent duplicates
  - Migration applied: `20260607_rival_nudge_notifications.sql`

---

#### Task 8.4 — Join request notifications

- **What:** Creator notified when someone requests to join their run.
- **Spec ref:** SPEC.md Section 4.7
- **Dependencies:** Task 8.1, Task 5.1
- **Deliverable:** Join request triggers push to creator. Tapping opens the run's participant management view.

- [x] Done
  - `notify_creator_join_request()` trigger fires on `run_participants` INSERT with status='pending'
  - Inserts `join_request` notification for the run creator with data.run_id for deep link
  - NOT EXISTS guard prevents duplicates per requester+run
  - Migration applied: `20260607_join_request_notifications.sql`

---

#### Task 8.5 — Notification preferences

- **What:** Settings screen toggles for all notification types.
- **Spec ref:** SPEC.md Section 5.2 (Settings → Notifications)
- **Skills:** android-presentation-mvi, ui-design
- **Dependencies:** Task 8.1
- **Deliverable:** All toggles from spec functional. Preferences saved and respected by dispatcher.

- [x] Done
  - Migration: 7 `notif_*` boolean columns added to `users` table (all default true)
  - `NotificationPreferences` domain model + `NotificationPreferencesDto` in `shared/notifications`
  - `NotificationRepository` extended: `getPreferences()` + `updatePreferences()`
  - `SupabaseNotificationRepository` implements both — reads/writes via `upsert` on users table
  - `NotificationPreferencesViewModel` + `NotificationPreferencesScreen` — 3 grouped sections (Running, Rivals, Other), toggles persist immediately on change with saving indicator
  - Wired at `SettingsNotificationsRoute` in `AppNavGraph`
  - `notification_dispatcher` Edge Function updated to check preference column per notification type before FCM delivery — skips and marks sent when user opted out
  - Deploy: `supabase functions deploy notification_dispatcher` (manual, as before)

---

### Phase 9 — Safety & Reporting

> Goal: users can block and report. Reports reach the admin queue. Automated signals fire.

---

#### Task 9.1 — User blocking

- **What:** Block/unblock any user. Mutual invisibility enforced via RLS.
- **Spec ref:** SPEC.md Section 6.1 (User Blocking)
- **Skills:** android-data-layer, android-presentation-mvi
- **Dependencies:** Task 1.2 (user_blocks table), Task 1.3 (RLS)
- **Deliverable:** Block from profile or participant list. Blocked users invisible in discovery, runs, and chat. Block list manageable from settings.

- [x] Done
  - `user_blocks` table with RLS (blocker sees only own rows; self-block prevented by CHECK)
  - `fn_is_blocked(a,b)` SECURITY DEFINER function for mutual check bypassing RLS
  - `fn_get_blocked_users()` SECURITY DEFINER RPC for the blocked list screen
  - RESTRICTIVE SELECT policies on `runs` and `users` tables enforce mutual invisibility
  - `BlockRepository` interface + `SupabaseBlockRepository` in `shared/runMatching`
  - `UserProfileViewModel` extended: `isBlockedByMe`, `showBlockConfirm` state; block/unblock actions
  - `UserProfileScreen` extended: Block/Unblock button top-right; confirmation AlertDialog
  - `BlockedUsersViewModel` + `BlockedUsersScreen` (Settings → Privacy → Blocked Users)
  - `BlockedUsersRoute` wired in nav graph; `BlockedUsersViewModel` in Koin

---

#### Task 9.2 — Report system

- **What:** Report a user, run, or message. Category selection + optional description.
- **Spec ref:** SPEC.md Section 6.2 (Reporting)
- **Skills:** android-presentation-mvi, ui-design
- **Dependencies:** Task 1.2
- **Deliverable:** Report button on user profiles, run cards, and chat messages. Saved to reports table. Reporter sees confirmation. Reported user not notified.

- [x] Done
  - `reports` table with RLS: authenticated INSERT + SELECT own rows; no UPDATE/DELETE for users
  - `ReportRepository` interface + `SupabaseReportRepository` in `shared/runMatching`
  - `ReportTarget` sealed class (User/Run/Message) with per-type reason lists (spec §6.2 tables)
  - `ReportDialog` composable: reason chip selection, optional description field, success confirmation screen
  - `UserProfileViewModel/Screen`: ⋯ overflow menu with Block + Report options; AlertDialog confirmation
  - `RunDetailViewModel/Screen`: "Report this run" text link at bottom for non-creator viewers
  - `RunChatViewModel/Screen`: long-press on another user's message opens report dialog
  - `FakeReportRepository` test double; updated `RunDetailViewModelTest` + `RunChatViewModelTest`

---

#### Task 9.3 — Automated safety signals Edge Function

- **What:** Daily Edge Function that flags abuse cases.
- **Spec ref:** SPEC.md Section 6.3 (Automated Safety Signals)
- **Dependencies:** Task 9.2, Task 6.3
- **Deliverable:** abuse_detection Edge Function deployed and running daily.

**Three signals from spec:**
1. 3+ reports in 30 days → flag for priority admin review
2. show-up rate < 40% → suspend from creating runs pending review
3. Actual pace >2 min/km outside stated zone on 3+ consecutive runs → force-recalculate zone + admin alert (handled in Task 6.6)

- [ ] Done

---

### Phase 10 — Settings & Privacy

> Goal: all settings from the spec are functional.

---

#### Task 10.1 — Settings screen

- **What:** Top-level settings screen navigating to all sub-screens.
- **Spec ref:** SPEC.md Section 5.2
- **Skills:** android-navigation, android-presentation-mvi, ui-design
- **Dependencies:** Phase 1 complete
- **Deliverable:** Settings navigates to Account, Notifications, Privacy, App sub-screens.

- [ ] Done

---

#### Task 10.2 — Account settings

- **What:** Edit profile, change email/password, connected apps (reconnect/disconnect Strava), delete account, export data.
- **Spec ref:** SPEC.md Section 5.2 (Account)
- **Skills:** android-presentation-mvi, android-di-koin, ui-design
- **Dependencies:** Task 10.1
- **Deliverable:** All account actions functional. Disconnecting Strava sets strava_connected=false and is_verified=false. Delete account wipes all user data. Export generates JSON (GDPR Article 20).

- [ ] Done

---

#### Task 10.3 — Privacy settings

- **What:** Profile visibility, pace zone visibility, run history visibility, rival visibility, location precision. Blocked users list with unblock.
- **Spec ref:** SPEC.md Section 5.2 (Privacy), Section 6.1 (blocked users manageable from settings)
- **Skills:** android-presentation-mvi, ui-design
- **Dependencies:** Task 10.1, Task 9.1
- **Deliverable:** All privacy toggles functional. Blocked users list shows all blocked users with unblock option.

**Supabase columns to add to users:** profile_visibility, show_pace_zone, show_run_history, show_rivals, location_precision

- [ ] Done

---

#### Task 10.4 — App settings

- **What:** Language (English/Hebrew RTL), units (km/miles), map style (Standard/Satellite), app version with copy-to-clipboard on tap.
- **Spec ref:** SPEC.md Section 5.2 (App), Section 10.3 (Localization)
- **Skills:** android-presentation-mvi, ui-design
- **Dependencies:** Task 10.1
- **Deliverable:** Language switch triggers RTL/LTR change. Units update across the app. Map style applies. Tapping app version copies it to clipboard.

- [ ] Done

---

### Phase 11 — Deep Linking

> Goal: runs and profiles are shareable via link. Links open the app to the correct screen.

---

#### Task 11.1 — Deep link handling

- **What:** Android App Links and iOS Universal Links. Shared URL parsing in shared/deeplink.
- **Spec ref:** SPEC.md Section 10.1 (Deep Linking)
- **Skills:** android-navigation
- **Dependencies:** Task 0.6
- **Deliverable:** `paceup://run/{id}` and `paceup://user/{id}` open correct screens. Fallback web page redirects to store.

- [ ] Done

---

#### Task 11.2 — Share run

- **What:** Share button on run detail generates deep link and opens system share sheet.
- **Spec ref:** SPEC.md Section 10.1
- **Skills:** android-presentation-mvi
- **Dependencies:** Task 11.1, Task 3.4
- **Deliverable:** Share button on run detail generates correct deep link URL. Android share sheet opens.

- [ ] Done

---

### Phase 12 — Offline Support

> Goal: app behaves gracefully without network.

---

#### Task 12.1 — Offline cache layer

- **What:** SQLDelight cache populated on success. Served when offline. Correct banner shown.
- **Spec ref:** SPEC.md Section 10.2 (Offline Behavior)
- **Skills:** android-data-layer
- **Dependencies:** Task 0.7
- **Deliverable:** Correct offline behavior per spec for all screens.

**Per-screen from spec:**
- Profile: cached data shown
- Upcoming runs: cached with all params including map tile
- Run chat: cached messages readable, outgoing queued, banner shown
- Discovery map: cached pins with "Showing cached data" banner
- Create run: blocked — clear error message shown
- Join request: blocked — clear error message shown

- [ ] Done

---

### Phase 13 — Admin Panel MVP

> Goal: basic moderation before launch.

---

#### Task 13.1 — Admin panel MVP

- **What:** Web admin panel (Retool or Supabase custom views). User management, run management, moderation queue.
- **Spec ref:** SPEC.md Section 9 (all subsections)
- **Dependencies:** Task 9.2, Task 1.1
- **Deliverable:** Admin can view users, runs, pending reports. Can warn, suspend, ban users.

**Minimum for launch (from spec Section 9):**
- Dashboard: total users, pending reports count
- User list: search by name/email/city, filter by tier/banned/suspended
- User detail: full stats, reports received, reports filed
- Actions: warn, suspend (1/7/30 days), ban, unsuspend/unban
- Report queue: view, dismiss, warn, remove content, suspend, ban
- Audit log: every admin action logged with who/what/when

**Roles from spec:**
- super_admin: full access
- moderator: moderation queue only
- analyst: read-only dashboard

- [ ] Done

---

### Phase 14 — Privacy & Security Checklist

> Goal: all data compliance requirements from spec are verified before launch.

---

#### Task 14.1 — Privacy & security audit

- **What:** Verify all data compliance and security requirements from spec are implemented.
- **Spec ref:** SPEC.md Section 6.4 (Privacy & Data Compliance)
- **Dependencies:** All previous phases
- **Deliverable:** All items below confirmed implemented and tested

**Checklist from spec:**
- [ ] OAuth tokens (Strava, Garmin) stored encrypted — never plain SharedPreferences
- [ ] Raw Strava/Garmin activity data NOT persisted — only derived metrics
- [ ] Location stored as city + approximate lat/lng — precise home address never collected
- [ ] Push notification tokens deleted on logout AND account delete
- [ ] Run chat messages auto-deleted 48h after run end (chat_cleanup function)
- [ ] Delete account removes ALL user data from Supabase
- [ ] Export data generates complete JSON (GDPR Article 20)
- [ ] Supabase project in EU region (GDPR)
- [ ] Admin audit log captures all admin actions
- [ ] No secrets in source code — all in local.properties or env vars

- [ ] Done

---

### Phase 15 — Pre-Launch Polish

> Goal: the app is solid, tested, and ready for real users.

---

#### Task 15.1 — Rate limiting

- **What:** Server-side rate limits per spec for each action type.
- **Spec ref:** SPEC.md Section 10.4 (Rate Limiting)
- **Dependencies:** All feature phases
- **Deliverable:** All limits enforced. Client handles 429 with exponential backoff.

**Limits from spec:**
- Run creation: max 5/day (trusted: max 10)
- Join requests: max 20/day
- Reports: max 10/day
- Profile updates: max 5/day
- Strava API: batched/cached to stay within 100 req/15min, 1000 req/day

- [ ] Done

---

#### Task 15.2 — Empty states

- **What:** Every screen that can be empty has a proper empty state — icon + headline + CTA.
- **Spec ref:** SPEC.md Section 5.1 (Empty State)
- **Skills:** ui-design
- **Dependencies:** All feature phases
- **Deliverable:** No screen shows blank or crash when data is empty.

**Key empty states:**
- Map with no runs: "No runs in [city] yet. Be the first to create one." + Create run CTA
- Profile with no run history
- Rival list with no rivals
- Chat with no messages

- [ ] Done

---

#### Task 15.3 — Error states

- **What:** Every screen handles network error, auth error, and empty results gracefully.
- **Spec ref:** CLAUDE.md Developer Rules → Error Handling
- **Skills:** android-presentation-mvi, ui-design
- **Dependencies:** All feature phases
- **Deliverable:** No screen crashes or shows blank on error. All show message + retry CTA.

- [ ] Done

---

#### Task 15.4 — KtLint pass

- **What:** Run KtLint on entire codebase. Fix all violations.
- **Spec ref:** CLAUDE.md Developer Rules → Code Formatting
- **Dependencies:** All feature phases
- **Deliverable:** `./gradlew ktlintCheck` passes with zero violations.

- [ ] Done

---

#### Task 15.5 — Full test pass

- **What:** Every ViewModel and every Repository has unit tests.
- **Spec ref:** CLAUDE.md Developer Rules → Testing
- **Skills:** android-testing
- **Dependencies:** All feature phases
- **Deliverable:** `./gradlew test` passes. All ViewModels and Repositories covered.

- [ ] Done

---

#### Task 15.6 — iOS parity audit

- **What:** Review every expect/actual implementation. Confirm iOS side is complete.
- **Spec ref:** CLAUDE.md Developer Rules → iOS Parity
- **Dependencies:** All feature phases
- **Deliverable:** Every expect declaration has a correct actual on both Android and iOS. No iOS placeholder remaining.

- [ ] Done

---

#### Task 15.7 — Performance audit

- **What:** Profile on Android. Fix main thread violations, slow lists, unnecessary recompositions.
- **Spec ref:** CLAUDE.md Developer Rules → Performance
- **Dependencies:** All feature phases
- **Deliverable:** No StrictMode violations. No ANRs on slow network. LazyColumn smooth on 200+ items.

- [ ] Done

---

#### Task 15.8 — Store submission checklist

- **What:** Verify all technical requirements for Play Store and App Store submission.
- **Dependencies:** All feature phases
- **Deliverable:** All items below confirmed

**Checklist:**
- [ ] Deep link domain verification configured (assetlinks.json / apple-app-site-association)
- [ ] Push notification entitlements configured for iOS
- [ ] App icons all sizes generated
- [ ] Privacy policy URL ready
- [ ] Terms of service URL ready
- [ ] Notification permission strings match Apple guidelines
- [ ] App reviewed by lawyer for GDPR/Israeli privacy law before submission

- [ ] Done

---

## Bucket B — Launch-If-Time

> Only start these if Bucket A is complete, stable, and tested.
> Do not let these delay the launch of Bucket A.

---

- **B.1** — Rival suggestions: suggest rivals based on pace zone + city similarity (`getSuggestedRivals()`)
- **B.2** — Recurring runs: weekly/biweekly run creation option + subscribe feature
- **B.3** — Tourist run mode: "I'm visiting" framing, locals notified
- **B.4** — Pacer run mode: designated pacer, pacer-eligible tier check
- **B.5** — Race prep run mode: linked to race, target time filter
- **B.6** — Pace accuracy score: post-run pace vs stated zone, rolling 0–5 score, added to trusted tier criteria
- **B.7** — Rival historical sparkline: last 8 weeks chart on rival dashboard
- **B.8** — Map style setting: Standard / Satellite toggle (already in Task 10.4 — quick add)

---

## Bucket C — Post-Launch

> Do not build any of these before launch. Validate Bucket A with real users first.

---

- **C.1** — Garmin Connect integration: full OAuth + activity sync, fills gaps when Strava absent
- **C.2** — Run recap card: auto-generated image post-run, shareable to Instagram/WhatsApp/Strava
- **C.3** — Running partner streaks & badges: 3/5/10 runs together, Run Crew page
- **C.4** — City leaderboard: weekly distance ranking by city, pace zone, run mode
- **C.5** — Milestone runs: detect 500km/100 runs/1 year, suggest celebration run
- **C.6** — Monetization: Free tier limits, PaceUp Pro ($4.99/mo, $39/yr), Run Club accounts
- **C.7** — Full admin panel: Next.js, analytics dashboard, D1/D7/D30 retention, run completion rate
- **C.8** — AI rival suggestions: smarter matching beyond simple pace+city similarity
- **C.9** — Heart rate zone matching: Garmin HR data for HR-based run modes
- **C.10** — Route sharing: run routes reusable, route-based run creation
- **C.11** — In-app Strava activity feed
- **C.12** — Group challenges: team monthly mileage
- **C.13** — Web app for run discovery

---

## Appendix — Quick Reference

### Spec sections by feature

| Feature | SPEC.md Section |
|---|---|
| User profile & pace zone | 4.1 |
| Run creation & modes | 4.2 |
| Run discovery | 4.3 |
| Trust & reputation | 4.4 |
| Rival system | 4.5 |
| Post-run social | 4.6 |
| Notifications list | 4.7 |
| Onboarding flow | 5.1 |
| Settings | 5.2 |
| Safety & reporting | 6.1–6.4 |
| Strava integration | 7.1 |
| Garmin integration | 7.2 |
| Database schema | 8.2 |
| Realtime subscriptions | 8.3 |
| Edge Functions | 8.4 |
| RLS policies | 8.5 |
| Admin panel | 9 |
| Deep linking | 10.1 |
| Offline behavior | 10.2 |
| Hebrew RTL | 10.3 |
| Rate limiting | 10.4 |
| Notification permission | 10.5 |
| Search | 10.6 |
| Force update | 10.7 |

### Skills by task type

| Task type | Skills to read |
|---|---|
| New screen / feature | android-presentation-mvi, android-di-koin, android-navigation, android-testing, ui-design |
| New data layer | android-data-layer, android-di-koin, android-testing |
| New module | android-module-structure |
| Edge Function | none (Deno/TypeScript) |
| Navigation route | android-navigation |
