package com.example.paceup.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.ui.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

private val Background   = Color(0xFF0D1B2A)
private val Surface      = Color(0xFF1A2E3E)
private val SurfaceAlt   = Color(0xFF1F3448)
private val PrimaryBlue  = Color(0xFF1D6FA8)
private val TextPrimary  = Color(0xFFE8EAF0)
private val TextSecondary = Color(0xFF8892A4)
private val Divider      = Color(0xFF243447)

// ─── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun NotificationPreferencesRoot(
    onNavigateBack: () -> Unit,
    viewModel: NotificationPreferencesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            NotificationPreferencesEvent.NavigateBack -> onNavigateBack()
        }
    }
    NotificationPreferencesScreen(state = state, onAction = viewModel::onAction)
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesScreen(
    state: NotificationPreferencesState,
    onAction: (NotificationPreferencesAction) -> Unit,
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Notifications", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onAction(NotificationPreferencesAction.OnBackClick) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("←", color = TextPrimary, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 16.dp),
                            color = PrimaryBlue,
                            strokeWidth = 2.dp,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (state.saveError) {
                Text(
                    text = "Failed to save. Check your connection.",
                    color = Color(0xFFA32D2D),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            PreferenceSection(title = "Running") {
                PrefToggle(
                    label = "Run reminders",
                    description = "24h and 2h before each run",
                    checked = state.prefs.runReminders,
                    onCheckedChange = { onAction(NotificationPreferencesAction.OnToggleRunReminders(it)) },
                )
                PrefDivider()
                PrefToggle(
                    label = "Join requests",
                    description = "When someone requests to join your run",
                    checked = state.prefs.joinRequests,
                    onCheckedChange = { onAction(NotificationPreferencesAction.OnToggleJoinRequests(it)) },
                )
                PrefDivider()
                PrefToggle(
                    label = "Partner ratings",
                    description = "Prompts to rate your run partners",
                    checked = state.prefs.partnerRatings,
                    onCheckedChange = { onAction(NotificationPreferencesAction.OnTogglePartnerRatings(it)) },
                )
                PrefDivider()
                PrefToggle(
                    label = "New matching runs",
                    description = "When a run matching your pace opens nearby",
                    checked = state.prefs.newRuns,
                    onCheckedChange = { onAction(NotificationPreferencesAction.OnToggleNewRuns(it)) },
                )
            }

            Spacer(Modifier.height(16.dp))

            PreferenceSection(title = "Rivals") {
                PrefToggle(
                    label = "Rival activity",
                    description = "When your rival completes a run",
                    checked = state.prefs.rivalNudges,
                    onCheckedChange = { onAction(NotificationPreferencesAction.OnToggleRivalNudges(it)) },
                )
                PrefDivider()
                PrefToggle(
                    label = "Weekly rival summary",
                    description = "Sunday evening recap of the week's competition",
                    checked = state.prefs.rivalSummary,
                    onCheckedChange = { onAction(NotificationPreferencesAction.OnToggleRivalSummary(it)) },
                )
            }

            Spacer(Modifier.height(16.dp))

            PreferenceSection(title = "Other") {
                PrefToggle(
                    label = "Marketing",
                    description = "App updates and announcements",
                    checked = state.prefs.marketing,
                    onCheckedChange = { onAction(NotificationPreferencesAction.OnToggleMarketing(it)) },
                )
            }
        }
    }
}

// ─── Components ───────────────────────────────────────────────────────────────

@Composable
private fun PreferenceSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface),
    ) {
        content()
    }
}

@Composable
private fun PrefToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(description, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceAlt,
            ),
        )
    }
}

@Composable
private fun PrefDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = Divider,
        thickness = 0.5.dp,
    )
}
