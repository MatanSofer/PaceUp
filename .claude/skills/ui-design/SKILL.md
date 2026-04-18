# PaceUp UI Design Skill

## Philosophy

PaceUp is a running app for people who take running seriously. The UI should feel like
the difference between a cheap sports watch and a Garmin — same data, completely different
emotional experience. Every screen should feel like it was designed by someone who actually
runs at 5am in the dark.

**The feeling we're after:**
- Dark energy meets clean data
- Fast, precise, confident — like a well-paced tempo run
- Premium sports brand (think Nike x Apple Watch) not generic fitness app
- Data is the hero — numbers should look beautiful, not clinical

**What we are NOT:**
- Not playful or game-like (no confetti, no cartoon characters)
- Not corporate / enterprise (no flat blue buttons on white backgrounds)
- Not Strava clone (Strava is a feed. We are a matchmaking + social layer)
- Not dark for the sake of dark — use darkness with purpose

---

## Color System

### Base Palette

```
Primary Blue:     #1A73E8   — trust, pace data, primary actions
Electric Blue:    #4FC3F7   — highlights, active states, live data
Deep Navy:        #0D1B2A   — dark backgrounds, deep surfaces
Midnight:         #111827   — darkest backgrounds
Charcoal:         #1F2937   — card surfaces on dark bg
Slate:            #374151   — elevated surfaces
```

```
Accent Orange:    #FF5722   — Strava energy, CTAs, high urgency
Amber:            #F59E0B   — warnings, late cancel, moderate
```

```
Success Green:    #10B981   — attended, on pace, trusted badge
Muted Green:      #065F46   — deep green backgrounds
```

```
Error Red:        #EF4444   — no-show, banned, danger
Soft Red:         #7F1D1D   — error backgrounds
```

```
Text White:       #F9FAFB   — primary text on dark
Text Muted:       #9CA3AF   — secondary text on dark
Text Faint:       #4B5563   — disabled, placeholder
```

### Pace Zone Colors — The Visual Language of Speed

Pace zones are the identity of every runner in PaceUp. They must be instantly
recognisable anywhere they appear — badges, pins, cards, profiles.

```
Zone A  (<4:30/km):    #A855F7  Purple   — elite
Zone B  (4:30–5:00):   #3B82F6  Blue     — advanced
Zone C  (5:00–5:30):   #10B981  Green    — intermediate
Zone D  (5:30–6:30):   #F59E0B  Amber    — recreational
Zone E  (>6:30/km):    #6B7280  Gray     — casual
```

### Show-Up Rate Colors

```
≥85%:   #10B981  Green   — trusted, reliable
70–84%: #F59E0B  Amber   — caution
<70%:   #EF4444  Red     — unreliable
```

### Dark / Light Mode

The app has TWO visual modes that are both first-class:

**Dark mode (default, flagship):**
- Background: #0D1B2A (Deep Navy)
- Surface: #1F2937 (Charcoal)
- Elevated surface: #374151 (Slate)
- Primary text: #F9FAFB
- Secondary text: #9CA3AF

**Light mode (outdoor / sunlight legibility):**
- Background: #F1F5F9
- Surface: #FFFFFF
- Elevated surface: #F8FAFC
- Primary text: #0D1B2A
- Secondary text: #64748B

Dark mode is the hero. Design dark first, light second.

---

## Typography

### Typeface

Use the system font stack — Inter on Android (via Compose), SF Pro on iOS.
Both are clean, athletic, and highly legible at speed.

```kotlin
// Compose font usage
FontFamily.Default  // resolves to Inter on Android, SF Pro on iOS
```

### Type Scale

```
Display XL:   48sp  Bold    — hero numbers (weekly km total, pace time)
Display L:    36sp  Bold    — screen headlines on hero cards
Headline 1:   28sp  Bold    — screen titles
Headline 2:   22sp  SemiBold — section headers
Headline 3:   18sp  SemiBold — card titles, names
Body L:       17sp  Regular  — primary body text
Body M:       15sp  Regular  — secondary body, descriptions
Body S:       13sp  Regular  — captions, timestamps, metadata
Label:        11sp  Medium   — badges, tags, chips (ALL CAPS)
Mono:         16sp  Medium   — pace display (use tabular nums)
```

### Pace & Stats Typography

Numbers that represent pace, distance, and time are the most important
text in the app. They must feel precise and athletic:

```kotlin
// Always use tabular nums for stats — prevents layout shift
TextStyle(
    fontFeatureSettings = "tnum",  // tabular numbers
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.5).sp      // tight tracking on big numbers
)
```

**Pace display format:** always `5:24 /km` — space before /km, never `5:24/km`

**Distance:** `12.4 km` — one decimal, space before unit

**Show-up rate:** `94%` — bold, color-coded, no decimal

---

## Elevation & Surfaces

PaceUp uses a **layered dark glass** approach — not flat Material, not heavy shadows.
Think: dark frosted glass layers stacked with subtle borders.

### Surface Hierarchy (dark mode)

```
Level 0 — Background:   #0D1B2A  — the void, map backgrounds
Level 1 — Base card:    #1F2937  — default card surface
Level 2 — Raised card:  #374151  — modals, elevated cards
Level 3 — Top surface:  #4B5563  — tooltips, dropdowns
```

### Borders Instead of Shadows

```kotlin
// Use borders, never elevation shadows
border = BorderStroke(1.dp, Color(0xFF374151))          // subtle — most cards
border = BorderStroke(1.dp, Color(0xFF4FC3F7).copy(alpha = 0.3f))  // glow — active/selected
border = BorderStroke(1.dp, paceZoneColor.copy(alpha = 0.5f))      // zone — pace zone cards
```

### The Glow Effect — Use Sparingly

For selected states, active runs, and live data — a soft colored glow border:

```kotlin
Modifier
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF4FC3F7).copy(alpha = 0.8f),
                Color(0xFF1A73E8).copy(alpha = 0.4f)
            )
        ),
        shape = RoundedCornerShape(16.dp)
    )
```

Only use on: active/live run cards, selected pace zone, your own profile.

---

## Shape & Radius

```
Large cards:      16dp   — run cards, profile card, rival card
Medium cards:     12dp   — list items, small cards
Buttons:          24dp   — fully rounded pill buttons for primary CTAs
Secondary buttons: 10dp  — outlined, slightly rounded
Chips/badges:     100dp  — fully pill shaped
Input fields:     10dp   — slightly rounded, not pill
Bottom sheets:    top 24dp, bottom 0dp
```

---

## Component Patterns

### Run Card

The most important component in the app. Shows on the map and in list view.

```
┌─────────────────────────────────────────────┐
│  [ZONE BADGE]  Title or "Easy Run"    [MODE] │
│  ────────────────────────────────────────── │
│  📍 Yarkon Park, Tel Aviv                   │
│  🕕 Tomorrow 6:00 AM                        │
│  🏃 5:00–5:30 /km  •  10 km                │
│  👥 4/8 runners                             │
│  ────────────────────────────────────────── │
│  [Avatar] [Avatar] [Avatar]   [JOIN →]      │
└─────────────────────────────────────────────┘
```

- Background: Level 1 surface (#1F2937)
- Left accent bar: 4dp wide, pace zone color
- Zone badge: pill, zone color background, white text, 11sp ALL CAPS
- Join button: filled, primary blue, pill shape
- Filling up fast: add a thin pulsing orange border (Hot Run state)

### Pace Zone Badge

Used everywhere — profiles, cards, run pins, participant lists.

```kotlin
// Zone badge component
Box(
    modifier = Modifier
        .background(zoneColor.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
        .border(1.dp, zoneColor.copy(alpha = 0.6f), RoundedCornerShape(100.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp)
) {
    Text(
        text = "ZONE ${zone}  •  ${paceRange}",
        style = LabelStyle,  // 11sp, Medium, ALL CAPS
        color = zoneColor
    )
}
```

### Runner Profile Card

```
┌────────────────────────────────────┐
│  [Avatar 56dp]  Matan R.           │
│                 ZONE B  4:30–5:00  │
│  ─────────────────────────────── │
│  94% show-up  •  47 runs  •  12 partners │
│  [kept pace] [great energy]        │
└────────────────────────────────────┘
```

- Avatar: circular, 56dp, with pace zone color ring border (2dp)
- Tags: small pills, muted background, subtle border

### Stats Display — The Hero Numbers

When displaying pace, distance, or weekly mileage prominently:

```kotlin
// Big stat block
Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
        text = "5:24",
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
        letterSpacing = (-1).sp,
        color = Color(0xFF4FC3F7)  // electric blue
    )
    Text(
        text = "/km avg pace",
        fontSize = 13.sp,
        color = Color(0xFF9CA3AF),  // muted
        letterSpacing = 1.sp        // wide tracking on labels
    )
}
```

### Map Pins

Run discovery is map-first. Pins must be immediately readable:

- Shape: teardrop pin with zone color fill
- Content: distance km or run mode icon inside
- Size: 44dp × 52dp — large enough to tap comfortably
- Selected state: slightly larger (48dp × 56dp) with white ring
- Cluster: circular badge, count number, gradient of mixed zone colors

### Bottom Sheet — Run Detail

When user taps a map pin, a bottom sheet slides up. This is the most
used interaction in the app:

- Handle bar at top: 4dp × 32dp, rounded, muted color
- Peek height: 40% of screen — shows key info
- Full height: 90% of screen — shows all details + join button
- Background: Level 2 surface (#374151)
- Top corners: 24dp radius

### Buttons

```
Primary CTA:      Filled, #1A73E8, white text, 24dp radius, 56dp height
Strava Connect:   Filled, #FC4C02, white text, 24dp radius, 56dp height
Secondary:        Outlined, 1dp border in primary blue, 10dp radius
Destructive:      Outlined, 1dp border in red, text in red
Text/Ghost:       No background, no border, colored text only
```

All buttons: full width in forms, wrap content in cards.

### Input Fields

```
Height:       56dp
Background:   Level 2 surface
Border:       1dp, muted by default → primary blue on focus
Radius:       10dp
Label:        floats above on focus (Material style)
Error state:  red border + red helper text below
```

### Notification / Toast

Never use system toasts. Use a custom snackbar that slides in from the bottom:

- Background: Level 3 surface with left color accent bar
- Success: green accent
- Warning: amber accent
- Error: red accent
- Auto-dismiss: 3 seconds

---

## Motion & Animation

### Principles

- **Fast and purposeful** — no animation longer than 400ms
- **Physics-based** — ease-in-out, never linear
- **Never decorative** — every animation has a functional reason
- **Respect reduced motion** — always check system accessibility settings

### Specific Animations

```kotlin
// Screen transitions — shared element or slide
// Use slide for list→detail, fade for modal→dismiss

// Card press feedback
Modifier.clickable {
    // scale down to 0.97 on press, spring back
    animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
}

// Pace zone badge — pulse when it's a new zone
// Show-up rate — count up from 0 on first load (300ms)
// Map pins — spring in from scale 0 when map loads
// Bottom sheet — spring physics on drag release

// Live data indicator (rival stats, run fill count)
// Subtle pulse on the number when it updates:
// scale 1.0 → 1.08 → 1.0 over 200ms
```

### Page Transitions

```
List → Detail:     Slide up (bottom sheet) or slide right (full screen)
Modal:             Fade in background + slide up sheet
Back navigation:   Slide right out (reverse of enter)
Tab switch:        Crossfade, 200ms
```

---

## Screen-Specific Patterns

### Discovery Map Screen

This is the HOME screen and the most important screen in the app.

- Map is full-bleed, edge to edge, behind the status bar
- Dark map style (Google Maps dark theme / Mapbox dark)
- Run pins overlaid on top with zone colors
- Floating search bar at top: pill shape, frosted glass background
- Filter chips below search bar: horizontal scroll, dark pill style
- Bottom sheet peek: shows 1.5 run cards to hint at scrollability
- FAB: "+" button to create a run, bottom right, primary blue, 56dp

### Profile Screen

- Hero section: full-width card with dark gradient overlay
- Avatar: large (88dp), centered, pace zone ring
- Stats bar: 3 columns — runs, partners, rivals — with big numbers
- Pace zone: large badge, prominent
- Show-up rate: large percentage, color-coded, with bar chart below
- Partner tags: horizontal scroll of pill badges
- Run history: vertical list of past runs

### Rival Screen

This should feel like a sports competition dashboard:

- Two avatars facing each other with VS in the middle
- Weekly distance: large numbers, one per side, live
- Leader highlighted: subtle glow background behind the leading stat
- Progress bar between them showing relative standing
- Streak indicator: flame icon with count if streak ≥ 3 weeks
- Historical graph: small sparkline of last 8 weeks

### Login / Onboarding Screens

These set the first impression — make them count:

- Full dark background (#0D1B2A)
- PaceUp wordmark: large, centered, white
- Tagline below: muted, italic
- Email/password fields: dark surface, blue focus
- CTA button: full width, primary blue, pill
- NO generic stock runner photos — use abstract pace/distance data viz
  as background decoration (faint, low opacity grid of pace numbers)

### Strava Connect Screen

- Strava orange (#FC4C02) as the accent for this screen
- Strava logo mark (use official assets)
- Benefit statement: "We use your real runs to match you with compatible runners"
- Big orange "Connect Strava" button
- "Skip for now" as small text link below — clearly secondary

---

## Iconography

- Use Material Symbols (rounded style) as the base icon set
- Custom icons needed: pace zone letters (A/B/C/D/E), PaceUp logo mark
- Icon size: 24dp standard, 20dp in dense lists, 32dp in hero sections
- Icon color: always matches the context (white on dark, zone color for zone icons)
- Never use emoji as icons in the UI — use proper icon assets

---

## Spacing System

```
4dp   — minimum gap, tight inline spacing
8dp   — small gap, between related elements
12dp  — medium gap, within a card
16dp  — standard gap, card padding, list item padding
20dp  — comfortable gap, section spacing within screen
24dp  — large gap, between sections
32dp  — xl gap, hero section padding
48dp  — screen-level top/bottom padding
```

Screen horizontal padding: always 16dp on both sides.

---

## Accessibility

- Minimum touch target: 48dp × 48dp — never smaller
- Color is never the only indicator — always pair color with icon or text
- Pace zone badges include both color AND letter (A, B, C, D, E)
- Show-up rate includes both color AND percentage number
- All interactive elements have content descriptions
- Support system font scaling — never use fixed sp that ignores scaling
- RTL: all layouts must mirror correctly in Hebrew

---

## What Impressive Looks Like in Practice

When Claude Code builds a screen, the result should pass this test:

1. **The map screen** — looks like a premium navigation app, not a Google Maps embed
2. **A run card** — instantly communicates pace zone, distance, time, and fill status
   without the user having to read anything carefully
3. **A profile** — show-up rate and pace zone are the first things you notice,
   not the avatar
4. **The rival screen** — feels like checking a sports scoreboard, creates competitive energy
5. **Login screen** — dark, confident, athletic — sets the right expectation for the app

If a screen looks like it could belong in a generic todo app or a bank app,
it is not good enough for PaceUp. Every screen should be recognisably a
premium running app at a glance.
