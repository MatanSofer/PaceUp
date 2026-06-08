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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.ui.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

// ── Palette ───────────────────────────────────────────────────────────────────

private val BgColor      = Color(0xFF0D1B2A)
private val Surface      = Color(0xFF1F2937)
private val SurfaceEl    = Color(0xFF374151)
private val Divider      = Color(0xFF374151)
private val TextPri      = Color(0xFFF9FAFB)
private val TextMut      = Color(0xFF9CA3AF)
private val AccentBlue   = Color(0xFF1D6FA8)
private val SuccessGreen = Color(0xFF0F6E56)

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun AppSettingsRoot(
    appVersion: String,
    onNavigateBack: () -> Unit,
    viewModel: AppSettingsViewModel = koinViewModel(
        parameters = { org.koin.core.parameter.parametersOf(appVersion) }
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AppSettingsEvent.NavigateBack -> onNavigateBack()
            is AppSettingsEvent.CopyToClipboard -> clipboardManager.setText(AnnotatedString(event.text))
        }
    }

    AppSettingsScreen(state = state, onAction = viewModel::onAction)
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun AppSettingsScreen(
    state: AppSettingsState,
    onAction: (AppSettingsAction) -> Unit,
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
                    .clickable { onAction(AppSettingsAction.OnBackClick) }
                    .padding(4.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "App",
                color = TextPri,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        HorizontalDivider(color = Divider, thickness = 0.5.dp)

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(20.dp))

                // ── Language ──────────────────────────────────────────────────
                AppSection(title = "Language", modifier = Modifier.padding(horizontal = 16.dp)) {
                    AppChipSelector(
                        label = "Interface language",
                        sublabel = "Changes the app language and text direction",
                        selected = state.language,
                        options = listOf("en" to "English", "he" to "עברית (Hebrew)"),
                        onSelect = { onAction(AppSettingsAction.OnLanguageChange(it)) },
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Units ─────────────────────────────────────────────────────
                AppSection(title = "Units", modifier = Modifier.padding(horizontal = 16.dp)) {
                    AppChipSelector(
                        label = "Distance units",
                        sublabel = "Used for pace, distance, and run summaries",
                        selected = state.units,
                        options = listOf("km" to "Kilometers", "miles" to "Miles"),
                        onSelect = { onAction(AppSettingsAction.OnUnitsChange(it)) },
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Map style ─────────────────────────────────────────────────
                AppSection(title = "Map style", modifier = Modifier.padding(horizontal = 16.dp)) {
                    AppChipSelector(
                        label = "Default map view",
                        sublabel = "Applies to run maps and location previews",
                        selected = state.mapStyle,
                        options = listOf("standard" to "Standard", "satellite" to "Satellite"),
                        onSelect = { onAction(AppSettingsAction.OnMapStyleChange(it)) },
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Save ──────────────────────────────────────────────────────
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (state.saveSuccess) {
                        Text(
                            text = "App settings saved",
                            color = SuccessGreen,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { onAction(AppSettingsAction.OnDismissSaveSuccess) }
                                .padding(bottom = 8.dp),
                        )
                    }
                    Button(
                        onClick = { onAction(AppSettingsAction.OnSave) },
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── App version ───────────────────────────────────────────────
                AppSection(title = "About", modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(AppSettingsAction.OnCopyVersion) }
                            .padding(vertical = 14.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "App version", color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(text = "Tap to copy", color = TextMut, fontSize = 12.sp)
                        }
                        Text(
                            text = state.appVersion.ifEmpty { "—" },
                            color = TextMut,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun AppSection(
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
private fun AppChipSelector(
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
