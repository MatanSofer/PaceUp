package com.example.paceup.feature.createrun

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.feature.home.formatPaceRange
import com.example.paceup.platform.LocationEffect
import com.example.paceup.shared.runmatching.domain.RunMode
import com.example.paceup.ui.ObserveAsEvents
import com.example.paceup.ui.asString
import org.koin.compose.viewmodel.koinViewModel

private val Background = Color(0xFF0D1B2A)
private val Surface = Color(0xFF1F2937)
private val SurfaceElevated = Color(0xFF374151)
private val Divider = Color(0xFF374151)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val PrimaryBlue = Color(0xFF1A73E8)
private val ElectricBlue = Color(0xFF4FC3F7)
private val ErrorRed = Color(0xFFEF4444)

// ── Root ──────────────────────────────────────────────────────────────────────

@Composable
fun CreateRunRoot(
    onNavigateBack: () -> Unit,
    onNavigateToRunDetail: (String) -> Unit,
    viewModel: CreateRunViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            CreateRunEvent.NavigateBack -> onNavigateBack()
            is CreateRunEvent.NavigateToRunDetail -> onNavigateToRunDetail(event.runId)
        }
    }

    CreateRunScreen(state = state, onAction = viewModel::onAction)
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun CreateRunScreen(
    state: CreateRunState,
    onAction: (CreateRunAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .navigationBarsPadding(),
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            TopBar(
                step = state.step,
                totalSteps = state.totalSteps,
                onBack = { onAction(CreateRunAction.OnPreviousStep) },
            )

            // ── Step progress ─────────────────────────────────────────────────
            StepProgressBar(current = state.step, total = state.totalSteps)

            Spacer(Modifier.height(4.dp))

            // ── Step content ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                when (state.step) {
                    1 -> Step1Mode(state = state, onAction = onAction)
                    2 -> Step2DateTime(state = state, onAction = onAction)
                    3 -> Step3Location(state = state, onAction = onAction)
                    4 -> Step4Details(state = state, onAction = onAction)
                    5 -> Step5Filters(state = state, onAction = onAction)
                    6 -> Step6JoinMode(state = state, onAction = onAction)
                    7 -> Step7Review(state = state)
                }

                // Inline error
                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = state.error.asString(),
                        color = ErrorRed,
                        fontSize = 14.sp,
                    )
                }
            }

            // ── Bottom action bar ─────────────────────────────────────────────
            BottomActionBar(state = state, onAction = onAction)
        }

        // Full-screen loading overlay
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(step: Int, totalSteps: Int, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        BackButton(onClick = onBack)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = stepTitle(step),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Step $step of $totalSteps",
                color = TextMuted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Surface)
            .border(1.dp, Divider, CircleShape)
            .clickable { onClick() },
    ) {
        Text(text = "←", color = TextPrimary, fontSize = 18.sp)
    }
}

private fun stepTitle(step: Int) = when (step) {
    1 -> "Run mode"
    2 -> "Date & time"
    3 -> "Meeting point"
    4 -> "Run details"
    5 -> "Participant filters"
    6 -> "Join mode"
    7 -> "Review & confirm"
    else -> "Create run"
}

// ── Step progress bar ─────────────────────────────────────────────────────────

@Composable
private fun StepProgressBar(current: Int, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(total) { index ->
            val filled = index < current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (filled) PrimaryBlue else SurfaceElevated),
            )
        }
    }
}

// ── Step 1 — Mode ─────────────────────────────────────────────────────────────

@Composable
private fun Step1Mode(state: CreateRunState, onAction: (CreateRunAction) -> Unit) {
    StepHeader(
        title = "What kind of run are you planning?",
        subtitle = "Choose the run mode that best describes your session.",
    )

    Spacer(Modifier.height(20.dp))

    MVP_RUN_MODES.forEach { mode ->
        ModeCard(
            mode = mode,
            selected = state.selectedMode == mode,
            onClick = { onAction(CreateRunAction.OnModeSelected(mode)) },
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ModeCard(mode: RunMode, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) PrimaryBlue else Divider
    val bgColor = if (selected) PrimaryBlue.copy(alpha = 0.12f) else Surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = modeIcon(mode), fontSize = 24.sp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = modeDisplayName(mode),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = modeDescription(mode),
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
            if (selected) {
                Spacer(Modifier.width(12.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                ) {
                    Text(text = "✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun modeIcon(mode: RunMode) = when (mode) {
    RunMode.EASY -> "😊"
    RunMode.TEMPO -> "⚡"
    RunMode.RECOVERY -> "🌿"
    else -> "🏃"
}

private fun modeDisplayName(mode: RunMode) = when (mode) {
    RunMode.EASY -> "Easy / Social"
    RunMode.TEMPO -> "Tempo / Training"
    RunMode.RECOVERY -> "Recovery"
    else -> mode.name
}

private fun modeDescription(mode: RunMode) = when (mode) {
    RunMode.EASY -> "Relaxed pace, wide range. Great for meetups and first-time connections."
    RunMode.TEMPO -> "Structured effort, narrow pace range. Serious training session."
    RunMode.RECOVERY -> "Explicitly slow. Low pressure, social and restorative."
    else -> ""
}

// ── Step 2 — Date & time ──────────────────────────────────────────────────────

@Composable
private fun Step2DateTime(state: CreateRunState, onAction: (CreateRunAction) -> Unit) {
    StepHeader(
        title = "When is the run?",
        subtitle = "Enter the date and start time.",
    )

    Spacer(Modifier.height(20.dp))

    FormField(
        label = "Date",
        placeholder = "YYYY-MM-DD  (e.g. 2026-06-15)",
        value = state.scheduledDate,
        keyboardType = KeyboardType.Number,
        onValueChange = { onAction(CreateRunAction.OnDateChanged(it)) },
    )

    Spacer(Modifier.height(12.dp))

    FormField(
        label = "Start time",
        placeholder = "HH:MM  (24-hour, e.g. 06:30)",
        value = state.scheduledTime,
        keyboardType = KeyboardType.Number,
        onValueChange = { onAction(CreateRunAction.OnTimeChanged(it)) },
    )
}

// ── Step 3 — Location ─────────────────────────────────────────────────────────

@Composable
private fun Step3Location(state: CreateRunState, onAction: (CreateRunAction) -> Unit) {
    // Auto-fill lat/lng once from device GPS — no-ops if permission not granted
    if (!state.gpsLoaded) {
        LocationEffect { latLng ->
            onAction(CreateRunAction.OnGpsLocationReceived(latLng.lat, latLng.lng))
        }
    }

    StepHeader(
        title = "Where do you meet?",
        subtitle = "Enter the meeting point address and coordinates.",
    )

    Spacer(Modifier.height(20.dp))

    FormField(
        label = "Meeting address",
        placeholder = "e.g. Gordon Beach entrance",
        value = state.meetingAddress,
        onValueChange = { onAction(CreateRunAction.OnAddressChanged(it)) },
    )

    Spacer(Modifier.height(12.dp))

    FormField(
        label = "City",
        placeholder = "e.g. Tel Aviv",
        value = state.city,
        onValueChange = { onAction(CreateRunAction.OnCityChanged(it)) },
    )

    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FormField(
            label = "Latitude",
            placeholder = "e.g. 32.08",
            value = state.meetingLatText,
            keyboardType = KeyboardType.Decimal,
            onValueChange = { onAction(CreateRunAction.OnLatChanged(it)) },
            modifier = Modifier.weight(1f),
        )
        FormField(
            label = "Longitude",
            placeholder = "e.g. 34.78",
            value = state.meetingLngText,
            keyboardType = KeyboardType.Decimal,
            onValueChange = { onAction(CreateRunAction.OnLngChanged(it)) },
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(12.dp))

    val gpsNote = if (state.gpsLoaded)
        "Your current location was filled in automatically. Adjust if your meeting point is different."
    else
        "Tip: open Google Maps, long-press your meeting point, and copy the coordinates shown."
    // TODO(paceup): replace lat/lng text input with map picker (Task post-MVP)
    InfoNote(text = gpsNote)
}

// ── Step 4 — Run details ──────────────────────────────────────────────────────

@Composable
private fun Step4Details(state: CreateRunState, onAction: (CreateRunAction) -> Unit) {
    StepHeader(
        title = "Run details",
        subtitle = "Distance or duration, pace range, and optional title.",
    )

    Spacer(Modifier.height(20.dp))

    FormField(
        label = "Title (optional)",
        placeholder = "e.g. Morning tempo along the promenade",
        value = state.title,
        onValueChange = { onAction(CreateRunAction.OnTitleChanged(it)) },
    )

    Spacer(Modifier.height(12.dp))

    FormField(
        label = "Description (optional)",
        placeholder = "Describe the route, vibe, or any notes",
        value = state.description,
        singleLine = false,
        onValueChange = { onAction(CreateRunAction.OnDescriptionChanged(it)) },
    )

    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FormField(
            label = "Distance (km)",
            placeholder = "e.g. 10",
            value = state.distanceKmText,
            keyboardType = KeyboardType.Decimal,
            onValueChange = { onAction(CreateRunAction.OnDistanceChanged(it)) },
            modifier = Modifier.weight(1f),
        )
        FormField(
            label = "Duration (min)",
            placeholder = "e.g. 60",
            value = state.durationMinText,
            keyboardType = KeyboardType.Number,
            onValueChange = { onAction(CreateRunAction.OnDurationChanged(it)) },
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(20.dp))

    // ── Pace range ────────────────────────────────────────────────────────────
    Text(text = "Pace range", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(4.dp))
    Text(
        text = formatPaceRange(state.paceMinSec, state.paceMaxSec),
        color = ElectricBlue,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(8.dp))

    Text(text = "Min pace (faster limit)", color = TextMuted, fontSize = 12.sp)
    Slider(
        value = state.paceMinSec.toFloat(),
        onValueChange = { onAction(CreateRunAction.OnPaceMinChanged(it.toInt())) },
        valueRange = 210f..480f,
        steps = 0,
        colors = SliderDefaults.colors(
            thumbColor = PrimaryBlue,
            activeTrackColor = PrimaryBlue,
            inactiveTrackColor = SurfaceElevated,
        ),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "3:30 /km", color = TextMuted, fontSize = 11.sp)
        Text(text = "8:00 /km", color = TextMuted, fontSize = 11.sp)
    }

    Spacer(Modifier.height(8.dp))

    Text(text = "Max pace (slower limit)", color = TextMuted, fontSize = 12.sp)
    Slider(
        value = state.paceMaxSec.toFloat(),
        onValueChange = { onAction(CreateRunAction.OnPaceMaxChanged(it.toInt())) },
        valueRange = 210f..480f,
        steps = 0,
        colors = SliderDefaults.colors(
            thumbColor = ElectricBlue,
            activeTrackColor = ElectricBlue,
            inactiveTrackColor = SurfaceElevated,
        ),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "3:30 /km", color = TextMuted, fontSize = 11.sp)
        Text(text = "8:00 /km", color = TextMuted, fontSize = 11.sp)
    }
}

// ── Step 5 — Filters ──────────────────────────────────────────────────────────

@Composable
private fun Step5Filters(state: CreateRunState, onAction: (CreateRunAction) -> Unit) {
    StepHeader(
        title = "Who can join?",
        subtitle = "Set optional requirements for participants.",
    )

    Spacer(Modifier.height(20.dp))

    FormField(
        label = "Max participants",
        placeholder = "e.g. 10 (leave blank for unlimited)",
        value = state.maxParticipantsText,
        keyboardType = KeyboardType.Number,
        onValueChange = { onAction(CreateRunAction.OnMaxParticipantsChanged(it)) },
    )

    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FormField(
            label = "Min age",
            placeholder = "e.g. 18",
            value = state.ageMinText,
            keyboardType = KeyboardType.Number,
            onValueChange = { onAction(CreateRunAction.OnAgeMinChanged(it)) },
            modifier = Modifier.weight(1f),
        )
        FormField(
            label = "Max age",
            placeholder = "e.g. 50",
            value = state.ageMaxText,
            keyboardType = KeyboardType.Number,
            onValueChange = { onAction(CreateRunAction.OnAgeMaxChanged(it)) },
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(16.dp))

    // ── Gender filter ─────────────────────────────────────────────────────────
    Text(text = "Gender", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("any" to "Any", "male" to "Men", "female" to "Women").forEach { (value, label) ->
            FilterChip(
                label = label,
                selected = state.genderFilter == value,
                onClick = { onAction(CreateRunAction.OnGenderFilterChanged(value)) },
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    // ── Verified only ─────────────────────────────────────────────────────────
    ToggleRow(
        label = "Verified runners only",
        subtitle = "Only runners with Strava-verified pace can join",
        checked = state.verifiedOnly,
        onToggle = { onAction(CreateRunAction.OnVerifiedOnlyToggled) },
    )
}

// ── Step 6 — Join mode ────────────────────────────────────────────────────────

@Composable
private fun Step6JoinMode(state: CreateRunState, onAction: (CreateRunAction) -> Unit) {
    StepHeader(
        title = "How do people join?",
        subtitle = "Control who can participate and how.",
    )

    Spacer(Modifier.height(20.dp))

    listOf(
        Triple("open", "Open", "Anyone can join instantly"),
        Triple("request", "Request", "You approve each participant"),
        Triple("invite_only", "Invite only", "Only people you invite can join"),
    ).forEach { (value, name, desc) ->
        JoinModeCard(
            value = value,
            name = name,
            description = desc,
            selected = state.joinMode == value,
            onClick = { onAction(CreateRunAction.OnJoinModeChanged(value)) },
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun JoinModeCard(
    value: String,
    name: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) PrimaryBlue else Divider
    val bgColor = if (selected) PrimaryBlue.copy(alpha = 0.12f) else Surface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(text = description, color = TextMuted, fontSize = 13.sp)
        }
        if (selected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
            ) {
                Text(text = "✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Step 7 — Review ───────────────────────────────────────────────────────────

@Composable
private fun Step7Review(state: CreateRunState) {
    StepHeader(
        title = "Review your run",
        subtitle = "Confirm the details before publishing.",
    )

    Spacer(Modifier.height(20.dp))

    ReviewCard {
        ReviewRow(label = "Mode", value = modeDisplayName(state.selectedMode ?: RunMode.EASY))
        ReviewDivider()
        ReviewRow(label = "Date", value = state.scheduledDate)
        ReviewDivider()
        ReviewRow(label = "Time", value = state.scheduledTime)
        ReviewDivider()
        ReviewRow(label = "Address", value = state.meetingAddress)
        ReviewDivider()
        ReviewRow(label = "City", value = state.city)
        ReviewDivider()
        ReviewRow(
            label = "Location",
            value = "${state.meetingLatText}, ${state.meetingLngText}",
        )
        ReviewDivider()
        ReviewRow(
            label = "Distance / Duration",
            value = buildString {
                if (state.distanceKmText.isNotBlank()) append("${state.distanceKmText} km")
                if (state.distanceKmText.isNotBlank() && state.durationMinText.isNotBlank()) append("  •  ")
                if (state.durationMinText.isNotBlank()) append("${state.durationMinText} min")
                if (isEmpty()) append("—")
            },
        )
        ReviewDivider()
        ReviewRow(
            label = "Pace range",
            value = formatPaceRange(state.paceMinSec, state.paceMaxSec),
        )
        ReviewDivider()
        ReviewRow(
            label = "Max participants",
            value = state.maxParticipantsText.ifBlank { "Unlimited" },
        )
        ReviewDivider()
        ReviewRow(label = "Gender", value = state.genderFilter.replaceFirstChar { it.uppercase() })
        ReviewDivider()
        ReviewRow(label = "Verified only", value = if (state.verifiedOnly) "Yes" else "No")
        ReviewDivider()
        ReviewRow(
            label = "Join mode",
            value = when (state.joinMode) {
                "open" -> "Open"
                "request" -> "Request"
                "invite_only" -> "Invite only"
                else -> state.joinMode
            },
        )
        if (state.title.isNotBlank()) {
            ReviewDivider()
            ReviewRow(label = "Title", value = state.title)
        }
    }
}

// ── Bottom action bar ─────────────────────────────────────────────────────────

@Composable
private fun BottomActionBar(state: CreateRunState, onAction: (CreateRunAction) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (state.step < state.totalSteps) {
            ActionButton(
                label = "Continue →",
                onClick = { onAction(CreateRunAction.OnNextStep) },
                enabled = !state.isLoading,
            )
        } else {
            ActionButton(
                label = "Publish run →",
                onClick = { onAction(CreateRunAction.OnCreateRun) },
                enabled = !state.isLoading,
                color = Color(0xFF10B981),
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = PrimaryBlue,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100.dp))
            .background(if (enabled) color else SurfaceElevated)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 16.dp),
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White else TextMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Shared sub-components ─────────────────────────────────────────────────────

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Text(
        text = title,
        color = TextPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = subtitle,
        color = TextMuted,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
}

@Composable
private fun FormField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, color = TextMuted.copy(alpha = 0.6f), fontSize = 14.sp) },
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Divider,
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                cursorColor = PrimaryBlue,
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) PrimaryBlue.copy(alpha = 0.2f) else Surface)
            .border(
                width = 1.dp,
                color = if (selected) PrimaryBlue else Divider,
                shape = RoundedCornerShape(100.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (selected) PrimaryBlue else TextMuted,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceElevated,
            ),
        )
    }
}

@Composable
private fun InfoNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PrimaryBlue.copy(alpha = 0.08f))
            .border(1.dp, PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "ℹ", color = PrimaryBlue, fontSize = 14.sp)
        Text(text = text, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun ReviewCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Divider, RoundedCornerShape(12.dp)),
    ) {
        content()
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp, modifier = Modifier.weight(0.4f))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@Composable
private fun ReviewDivider() {
    HorizontalDivider(color = Divider, thickness = 0.5.dp)
}
