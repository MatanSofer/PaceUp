package com.example.paceup.feature.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.ui.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

// ── Palette ───────────────────────────────────────────────────────────────────

private val BgColor    = Color(0xFF0D1B2A)
private val Surface    = Color(0xFF1F2937)
private val SurfaceEl  = Color(0xFF374151)
private val Divider    = Color(0xFF374151)
private val TextPri    = Color(0xFFF9FAFB)
private val TextMut    = Color(0xFF9CA3AF)
private val AccentBlue = Color(0xFF1D6FA8)
private val SuccessGreen = Color(0xFF0F6E56)

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun PrivacySettingsRoot(
    onNavigateBack: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    viewModel: PrivacySettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            PrivacySettingsEvent.NavigateBack          -> onNavigateBack()
            PrivacySettingsEvent.NavigateToBlockedUsers -> onNavigateToBlockedUsers()
        }
    }

    PrivacySettingsScreen(state = state, onAction = viewModel::onAction)
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun PrivacySettingsScreen(
    state: PrivacySettingsState,
    onAction: (PrivacySettingsAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor),
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = "←",
                color = TextPri,
                fontSize = 20.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAction(PrivacySettingsAction.OnBackClick) }
                    .padding(4.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Privacy",
                color = TextPri,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        HorizontalDivider(color = Divider, thickness = 0.5.dp)

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(20.dp))

                // ── Profile visibility ────────────────────────────────────────
                PrivacySection(
                    title = "Profile",
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    VisibilitySelector(
                        label = "Profile visibility",
                        sublabel = "Who can view your full profile",
                        selected = state.profileVisibility,
                        options = listOf(
                            "public"  to "Public",
                            "friends" to "Friends",
                            "private" to "Private",
                        ),
                        onSelect = { onAction(PrivacySettingsAction.OnProfileVisibilityChange(it)) },
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Visible on profile ────────────────────────────────────────
                PrivacySection(
                    title = "Visible on profile",
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    PrivacyToggleRow(
                        label = "Pace zone",
                        sublabel = "Show your pace zone badge",
                        checked = state.showPaceZone,
                        onCheckedChange = { onAction(PrivacySettingsAction.OnShowPaceZoneChange(it)) },
                    )
                    HorizontalDivider(color = Divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                    PrivacyToggleRow(
                        label = "Run history",
                        sublabel = "Show past PaceUp runs",
                        checked = state.showRunHistory,
                        onCheckedChange = { onAction(PrivacySettingsAction.OnShowRunHistoryChange(it)) },
                    )
                    HorizontalDivider(color = Divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                    PrivacyToggleRow(
                        label = "Rival connections",
                        sublabel = "Show your rival network",
                        checked = state.showRivals,
                        onCheckedChange = { onAction(PrivacySettingsAction.OnShowRivalsChange(it)) },
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Location ──────────────────────────────────────────────────
                PrivacySection(
                    title = "Location",
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    VisibilitySelector(
                        label = "Location precision",
                        sublabel = "How precisely your location is shown",
                        selected = state.locationPrecision,
                        options = listOf(
                            "city"    to "Show city",
                            "country" to "Show country only",
                        ),
                        onSelect = { onAction(PrivacySettingsAction.OnLocationPrecisionChange(it)) },
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Blocked users ─────────────────────────────────────────────
                PrivacySection(
                    title = "Blocked users",
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(PrivacySettingsAction.OnNavigateToBlockedUsers) }
                            .padding(vertical = 14.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Manage blocked users", color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(text = "View and unblock users", color = TextMut, fontSize = 12.sp)
                        }
                        Text(text = "›", color = TextMut, fontSize = 20.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Save ──────────────────────────────────────────────────────
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (state.saveSuccess) {
                        Text(
                            text = "Privacy settings saved",
                            color = SuccessGreen,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { onAction(PrivacySettingsAction.OnDismissSaveSuccess) }
                                .padding(bottom = 8.dp),
                        )
                    }
                    Button(
                        onClick = { onAction(PrivacySettingsAction.OnSave) },
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun PrivacySection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            color = TextMut,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(0.5.dp, Divider, RoundedCornerShape(12.dp))
                .background(Surface)
                .padding(horizontal = 16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun PrivacyToggleRow(
    label: String,
    sublabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = sublabel, color = TextMut, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentBlue,
                uncheckedThumbColor = TextMut,
                uncheckedTrackColor = SurfaceEl,
            ),
        )
    }
}

@Composable
private fun VisibilitySelector(
    label: String,
    sublabel: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Text(text = label, color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(text = sublabel, color = TextMut, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { (value, displayText) ->
                val isSelected = selected == value
                Text(
                    text = displayText,
                    color = if (isSelected) Color.White else TextMut,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AccentBlue else SurfaceEl)
                        .clickable { onSelect(value) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
