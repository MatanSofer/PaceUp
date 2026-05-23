package com.example.paceup.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.platform.PaceUpMap
import com.example.paceup.shared.runmatching.domain.RunMode
import com.example.paceup.ui.ObserveAsEvents
import com.example.paceup.ui.asString
import org.koin.compose.viewmodel.koinViewModel

private val BackgroundColor = Color(0xFF0D1B2A)
private val SurfaceColor = Color(0xFF1F2937)
private val SurfaceElevated = Color(0xFF374151)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val PrimaryBlue = Color(0xFF1A73E8)

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun HomeRoot(
    onNavigateToRunDetail: (String) -> Unit = {},
    onNavigateToCreateRun: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.NavigateToRunDetail -> onNavigateToRunDetail(event.runId)
            HomeEvent.NavigateToCreateRun -> onNavigateToCreateRun()
        }
    }

    HomeScreen(
        state = state,
        onAction = viewModel::onAction,
        onJoinRunClick = viewModel::onJoinRunClick,
        onCreateRunClick = viewModel::onCreateRunClick,
    )
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: MapDiscoveryState,
    onAction: (HomeAction) -> Unit,
    onJoinRunClick: (String) -> Unit,
    onCreateRunClick: () -> Unit,
) {
    val mapCenter = state.userLocation ?: DefaultMapCenter

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Full-bleed map ──────────────────────────────────────────────────
        PaceUpMap(
            runs = state.runs,
            selectedRunId = state.selectedRun?.id,
            userLocation = state.userLocation,
            center = mapCenter,
            onPinClick = { onAction(HomeAction.OnPinClick(it)) },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Top overlay: search bar + filter chips ──────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(top = 12.dp),
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { onAction(HomeAction.OnSearchQueryChange(it)) },
                onSubmit = { onAction(HomeAction.OnSearchSubmit) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            FilterChipRow(
                activeModes = state.activeModeFilters,
                verifiedOnly = state.verifiedOnlyFilter,
                onModeToggle = { onAction(HomeAction.OnModeFilterToggle(it)) },
                onVerifiedOnlyToggle = { onAction(HomeAction.OnVerifiedOnlyToggle) },
            )
        }

        // ── Loading indicator ───────────────────────────────────────────────
        if (state.isLoading) {
            CircularProgressIndicator(
                color = PrimaryBlue,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // ── Error toast ─────────────────────────────────────────────────────
        state.error?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF7F1D1D))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = error.asString(),
                    color = Color(0xFFFCA5A5),
                    fontSize = 14.sp,
                )
            }
        }

        // ── Discovery tooltip ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.showTooltip,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(top = 120.dp),
        ) {
            DiscoveryTooltip(onDismiss = { onAction(HomeAction.OnTooltipDismissed) })
        }

        // ── FAB: create run ─────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(PrimaryBlue)
                .clickable { onCreateRunClick() }
        ) {
            Text(
                text = "+",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                lineHeight = 28.sp,
            )
        }
    }

    // ── Bottom sheet: selected run detail ───────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    if (state.selectedRun != null) {
        ModalBottomSheet(
            onDismissRequest = { onAction(HomeAction.OnBottomSheetDismiss) },
            sheetState = sheetState,
            containerColor = SurfaceElevated,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = "Run Details",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(16.dp))
                RunCard(
                    run = state.selectedRun,
                    onJoinClick = { id ->
                        onAction(HomeAction.OnBottomSheetDismiss)
                        onJoinRunClick(id)
                    },
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Search bar ────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Search runs, city, neighbourhood…",
                color = TextMuted,
                fontSize = 15.sp,
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(100.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SurfaceColor.copy(alpha = 0.95f),
            unfocusedContainerColor = SurfaceColor.copy(alpha = 0.9f),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = PrimaryBlue,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

// ── Filter chips ──────────────────────────────────────────────────────────────

private val filterModes = listOf(
    RunMode.EASY, RunMode.TEMPO, RunMode.RACE_PREP, RunMode.RECOVERY, RunMode.TOURIST
)

@Composable
private fun FilterChipRow(
    activeModes: List<RunMode>,
    verifiedOnly: Boolean,
    onModeToggle: (RunMode) -> Unit,
    onVerifiedOnlyToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            label = "✓ Verified",
            isActive = verifiedOnly,
            activeColor = Color(0xFF10B981),
            onClick = onVerifiedOnlyToggle,
        )
        filterModes.forEach { mode ->
            FilterChip(
                label = mode.chipLabel(),
                isActive = mode in activeModes,
                activeColor = PrimaryBlue,
                onClick = { onModeToggle(mode) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    val bg = if (isActive) activeColor else SurfaceColor.copy(alpha = 0.9f)
    val border = if (isActive) activeColor else Color(0xFF374151)
    val text = if (isActive) Color.White else TextMuted

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(100.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = text,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun RunMode.chipLabel() = when (this) {
    RunMode.EASY -> "Easy"
    RunMode.TEMPO -> "Tempo"
    RunMode.RACE_PREP -> "Race Prep"
    RunMode.RECOVERY -> "Recovery"
    RunMode.TOURIST -> "Tourist"
    RunMode.PACER -> "Pacer"
}

// ── Tooltip ───────────────────────────────────────────────────────────────────

@Composable
private fun DiscoveryTooltip(onDismiss: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(SurfaceColor, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF374151), RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onDismiss() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = "Runs matched to your pace",
            color = PrimaryBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "These runs match your pace. Tap any pin to join.",
            color = TextPrimary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tap to dismiss",
            color = TextMuted,
            fontSize = 12.sp,
        )
    }
}
