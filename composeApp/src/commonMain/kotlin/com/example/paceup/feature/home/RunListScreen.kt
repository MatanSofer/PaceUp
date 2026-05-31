package com.example.paceup.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.shared.pacezone.PaceZone
import com.example.paceup.shared.runmatching.domain.RunMode
import com.example.paceup.ui.ObserveAsEvents
import com.example.paceup.ui.asString
import org.koin.compose.viewmodel.koinViewModel

private val BackgroundColor = Color(0xFF0D1B2A)
private val SurfaceColor = Color(0xFF1F2937)
private val SurfaceElevated = Color(0xFF374151)
private val DividerColor = Color(0xFF374151)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val PrimaryBlue = Color(0xFF1A73E8)

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun RunListRoot(
    onNavigateToRunDetail: (String) -> Unit = {},
    onNavigateToCreateRun: () -> Unit = {},
    tabBarOffset: Dp = 0.dp,
    viewModel: RunListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RunListEvent.NavigateToRunDetail -> onNavigateToRunDetail(event.runId)
        }
    }

    RunListScreen(
        state = state,
        onAction = viewModel::onAction,
        onCreateRunClick = onNavigateToCreateRun,
        tabBarOffset = tabBarOffset,
    )
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunListScreen(
    state: RunListState,
    onAction: (RunListAction) -> Unit,
    onCreateRunClick: () -> Unit,
    tabBarOffset: Dp = 0.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(top = tabBarOffset),
        ) {
            Spacer(Modifier.height(8.dp))

            // Sort + filter bar
            SortFilterBar(
                sortOrder = state.sortOrder,
                activeFilterCount = state.filters.activeCount,
                onSortChange = { onAction(RunListAction.OnSortOrderChange(it)) },
                onFiltersClick = { onAction(RunListAction.OnToggleFilterSheet) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

            // Result count
            if (!state.isLoading) {
                Text(
                    text = when {
                        state.runs.isEmpty() -> "No runs found"
                        else -> "${state.runs.size} run${if (state.runs.size != 1) "s" else ""} nearby"
                    },
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            // Run list
            if (state.runs.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.runs, key = { it.id }) { run ->
                        RunCard(
                            run = run,
                            scheduledTimeDisplay = formatScheduledAt(run.scheduledAt),
                            onJoinClick = { onAction(RunListAction.OnRunClick(it)) },
                        )
                    }
                    item { Spacer(Modifier.navigationBarsPadding().height(88.dp)) }
                }
            } else if (!state.isLoading) {
                EmptyRunsState(
                    hasActiveFilters = state.filters.activeCount > 0,
                    onClearFilters = { onAction(RunListAction.OnClearFilters) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        // Loading indicator
        AnimatedVisibility(
            visible = state.isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }

        // Error toast
        state.error?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF7F1D1D))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(text = error.asString(), color = Color(0xFFFCA5A5), fontSize = 14.sp)
            }
        }

        // FAB: create run
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(PrimaryBlue)
                .clickable { onCreateRunClick() },
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

    // Filter bottom sheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (state.showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { onAction(RunListAction.OnToggleFilterSheet) },
            sheetState = sheetState,
            containerColor = SurfaceElevated,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            FilterSheetContent(
                filters = state.filters,
                onAction = onAction,
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

// ── Sort + filter bar ─────────────────────────────────────────────────────────

@Composable
private fun SortFilterBar(
    sortOrder: RunSortOrder,
    activeFilterCount: Int,
    onSortChange: (RunSortOrder) -> Unit,
    onFiltersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Sort segments
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(SurfaceColor)
                .border(1.dp, DividerColor, RoundedCornerShape(100.dp)),
        ) {
            SortChip(
                label = "Soonest",
                isActive = sortOrder == RunSortOrder.SOONEST,
                onClick = { onSortChange(RunSortOrder.SOONEST) },
            )
            SortChip(
                label = "Closest",
                isActive = sortOrder == RunSortOrder.CLOSEST,
                onClick = { onSortChange(RunSortOrder.CLOSEST) },
            )
        }

        Spacer(Modifier.weight(1f))

        // Filters button
        val filterLabel = if (activeFilterCount > 0) "Filters · $activeFilterCount" else "Filters"
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (activeFilterCount > 0) PrimaryBlue.copy(alpha = 0.2f) else SurfaceColor)
                .border(
                    1.dp,
                    if (activeFilterCount > 0) PrimaryBlue.copy(alpha = 0.6f) else DividerColor,
                    RoundedCornerShape(100.dp),
                )
                .clickable { onFiltersClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = filterLabel,
                color = if (activeFilterCount > 0) PrimaryBlue else TextMuted,
                fontSize = 13.sp,
                fontWeight = if (activeFilterCount > 0) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun SortChip(label: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (isActive) PrimaryBlue else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else TextMuted,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyRunsState(
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "🏃", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (hasActiveFilters) "No runs match your filters" else "No runs nearby",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasActiveFilters) "Try adjusting or clearing your filters."
            else "Be the first to create a run in your area.",
            color = TextMuted,
            fontSize = 14.sp,
        )
        if (hasActiveFilters) {
            Spacer(Modifier.height(20.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceColor)
                    .border(1.dp, DividerColor, RoundedCornerShape(24.dp))
                    .clickable { onClearFilters() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(text = "Clear filters", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Filter sheet ──────────────────────────────────────────────────────────────

@Composable
private fun FilterSheetContent(
    filters: RunListFilters,
    onAction: (RunListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Filters", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (filters.activeCount > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFF374151))
                        .clickable { onAction(RunListAction.OnClearFilters) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(text = "Clear all", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Pace Zone
        FilterSection(title = "Pace Zone") {
            FilterChipRow {
                PaceZone.entries.forEach { zone ->
                    val zoneColor = zone.color()
                    val isActive = zone in filters.paceZones
                    FilterChip(
                        label = "Zone ${zone.name}",
                        isActive = isActive,
                        activeColor = zoneColor,
                        onClick = { onAction(RunListAction.OnPaceZoneToggle(zone)) },
                    )
                }
            }
        }

        FilterDivider()

        // Run Mode
        FilterSection(title = "Run Mode") {
            FilterChipRow {
                RunMode.entries.forEach { mode ->
                    FilterChip(
                        label = mode.displayLabel(),
                        isActive = mode in filters.modes,
                        onClick = { onAction(RunListAction.OnModeToggle(mode)) },
                    )
                }
            }
        }

        FilterDivider()

        // Distance from me
        FilterSection(title = "Distance from me") {
            FilterChipRow {
                listOf(null to "Any", 5.0 to "5 km", 10.0 to "10 km", 20.0 to "20 km")
                    .forEach { (km, label) ->
                        FilterChip(
                            label = label,
                            isActive = filters.proximityKm == km,
                            onClick = { onAction(RunListAction.OnProximityChange(km)) },
                        )
                    }
            }
        }

        FilterDivider()

        // Run distance
        FilterSection(title = "Run distance") {
            FilterChipRow {
                listOf(null to "Any", 3f to "3 km+", 5f to "5 km+", 10f to "10 km+")
                    .forEach { (km, label) ->
                        FilterChip(
                            label = label,
                            isActive = filters.runMinDistanceKm == km,
                            onClick = { onAction(RunListAction.OnRunMinDistanceChange(km)) },
                        )
                    }
            }
        }

        FilterDivider()

        // Date
        FilterSection(title = "Date") {
            FilterChipRow {
                listOf(
                    DatePreset.ANY to "Any time",
                    DatePreset.TODAY_ONWARDS to "Today +",
                    DatePreset.NEXT_7_DAYS to "This week",
                ).forEach { (preset, label) ->
                    FilterChip(
                        label = label,
                        isActive = filters.datePreset == preset,
                        onClick = { onAction(RunListAction.OnDatePresetChange(preset)) },
                    )
                }
            }
        }

        FilterDivider()

        // Toggle switches
        FilterSection(title = "Other") {
            ToggleRow(
                label = "Verified runners only",
                checked = filters.verifiedOnly,
                onToggle = { onAction(RunListAction.OnVerifiedOnlyToggle) },
            )
            Spacer(Modifier.height(12.dp))
            ToggleRow(
                label = "Open join only",
                checked = filters.openJoinOnly,
                onToggle = { onAction(RunListAction.OnOpenJoinOnlyToggle) },
            )
            Spacer(Modifier.height(12.dp))
            ToggleRow(
                label = "Recurring runs only",
                checked = filters.recurringOnly,
                onToggle = { onAction(RunListAction.OnRecurringOnlyToggle) },
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        color = TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
    )
    Spacer(Modifier.height(10.dp))
    content()
}

@Composable
private fun FilterDivider() {
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun FilterChipRow(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun FilterChip(
    label: String,
    isActive: Boolean,
    activeColor: Color = PrimaryBlue,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.2f) else SurfaceColor)
            .border(
                1.dp,
                if (isActive) activeColor else DividerColor,
                RoundedCornerShape(100.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (isActive) activeColor else TextMuted,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = TextPrimary, fontSize = 15.sp)
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceColor,
                uncheckedBorderColor = DividerColor,
            ),
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Parses an ISO 8601 scheduledAt string and returns a display-friendly format.
 * Input: "2026-05-25T06:00:00" → Output: "May 25 · 6:00 AM"
 */
fun formatScheduledAt(scheduledAt: String): String {
    return try {
        val datePart = scheduledAt.substring(0, 10)
        val timePart = scheduledAt.substring(11, 16)
        val (_, month, day) = datePart.split("-").map { it.toInt() }
        val monthName = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[month - 1]
        val (hour, minute) = timePart.split(":").map { it.toInt() }
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "$monthName $day · $displayHour:${minute.toString().padStart(2, '0')} $amPm"
    } catch (_: Exception) {
        scheduledAt.take(16).replace("T", " ")
    }
}

private fun RunMode.displayLabel() = when (this) {
    RunMode.EASY -> "Easy"
    RunMode.TEMPO -> "Tempo"
    RunMode.RACE_PREP -> "Race Prep"
    RunMode.RECOVERY -> "Recovery"
    RunMode.TOURIST -> "Tourist"
    RunMode.PACER -> "Pacer"
}
