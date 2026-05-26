package com.example.paceup.feature.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.feature.home.RunCard
import com.example.paceup.feature.home.ZoneBadge
import com.example.paceup.feature.home.color
import com.example.paceup.shared.pacezone.PaceZone
import com.example.paceup.shared.runmatching.domain.UserSummary
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
fun SearchRoot(
    onNavigateBack: () -> Unit,
    onNavigateToRunDetail: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SearchEvent.NavigateToRunDetail -> onNavigateToRunDetail(event.runId)
            is SearchEvent.NavigateToUserProfile -> onNavigateToUserProfile(event.userId)
            SearchEvent.NavigateBack -> onNavigateBack()
        }
    }

    SearchScreen(state = state, onAction = viewModel::onAction)
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun SearchScreen(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Auto-focus the search field when the screen opens
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .statusBarsPadding(),
    ) {
        // ── Top bar: back + search field ────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // Back button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceColor)
                    .clickable { onAction(SearchAction.OnBackClick) },
            ) {
                Text(
                    text = "←",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.width(10.dp))

            // Search text field
            TextField(
                value = state.query,
                onValueChange = { onAction(SearchAction.OnQueryChange(it)) },
                placeholder = {
                    Text(
                        text = when (state.activeTab) {
                            SearchTab.RUNS -> "Search runs, city, neighbourhood…"
                            SearchTab.PEOPLE -> "Search by name…"
                        },
                        color = TextMuted,
                        fontSize = 15.sp,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(100.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = PrimaryBlue,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        Text(
                            text = "✕",
                            color = TextMuted,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { onAction(SearchAction.OnClearQuery) }
                                .padding(end = 12.dp),
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
        }

        // ── Tab bar: Runs | People ──────────────────────────────────────────
        SearchTabBar(
            activeTab = state.activeTab,
            onTabSelected = { onAction(SearchAction.OnTabChange(it)) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(4.dp))

        // ── Results ─────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    color = PrimaryBlue,
                    modifier = Modifier.align(Alignment.Center),
                )

                state.query.isBlank() -> SearchEmptyPrompt(tab = state.activeTab)

                state.activeTab == SearchTab.RUNS && state.runResults.isEmpty() ->
                    NoResultsState(query = state.query)

                state.activeTab == SearchTab.PEOPLE && state.userResults.isEmpty() ->
                    NoResultsState(query = state.query)

                else -> ResultsList(state = state, onAction = onAction)
            }

            // Error toast
            state.error?.let { error ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
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
        }
    }
}

// ── Tab bar ───────────────────────────────────────────────────────────────────

@Composable
private fun SearchTabBar(
    activeTab: SearchTab,
    onTabSelected: (SearchTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(SurfaceColor)
            .border(1.dp, SurfaceElevated, RoundedCornerShape(100.dp)),
    ) {
        SearchTab.entries.forEach { tab ->
            val isActive = tab == activeTab
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isActive) PrimaryBlue else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 28.dp, vertical = 10.dp),
            ) {
                Text(
                    text = tab.label(),
                    color = if (isActive) Color.White else TextMuted,
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

private fun SearchTab.label() = when (this) {
    SearchTab.RUNS -> "Runs"
    SearchTab.PEOPLE -> "People"
}

// ── Results list ──────────────────────────────────────────────────────────────

@Composable
private fun ResultsList(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
        ),
    ) {
        when (state.activeTab) {
            SearchTab.RUNS -> items(state.runResults, key = { it.id }) { run ->
                RunCard(
                    run = run,
                    onJoinClick = { onAction(SearchAction.OnRunClick(it)) },
                )
            }
            SearchTab.PEOPLE -> items(state.userResults, key = { it.id }) { user ->
                UserResultCard(
                    user = user,
                    onClick = { onAction(SearchAction.OnUserClick(user.id)) },
                )
            }
        }
    }
}

// ── User result card ──────────────────────────────────────────────────────────

@Composable
private fun UserResultCard(
    user: UserSummary,
    onClick: () -> Unit,
) {
    val zone = user.paceZone?.let { letter ->
        runCatching { PaceZone.valueOf(letter) }.getOrNull()
    }
    val zoneColor = zone?.color() ?: Color(0xFF6B7280)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, SurfaceElevated, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // Avatar placeholder with pace zone ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceElevated)
                .border(2.dp, zoneColor, CircleShape),
        ) {
            Text(
                text = user.displayName.take(1).uppercase(),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            if (zone != null) {
                ZoneBadge(
                    zone = zone.name,
                    paceRange = zone.displayRange,
                    color = zoneColor,
                )
            }
        }

        // Show-up rate
        user.showUpRate?.let { rate ->
            val rateColor = when {
                rate >= 0.85f -> Color(0xFF10B981)
                rate >= 0.70f -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${(rate * 100).toInt()}%",
                    color = rateColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFeatureSettings = "tnum",
                    ),
                )
                Text(
                    text = "show-up",
                    color = TextMuted,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}

// ── Empty / prompt states ─────────────────────────────────────────────────────

@Composable
private fun SearchEmptyPrompt(tab: SearchTab) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp),
    ) {
        Text(
            text = when (tab) {
                SearchTab.RUNS -> "🔍"
                SearchTab.PEOPLE -> "👤"
            },
            fontSize = 48.sp,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = when (tab) {
                SearchTab.RUNS -> "Find a run"
                SearchTab.PEOPLE -> "Find a runner"
            },
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (tab) {
                SearchTab.RUNS -> "Search by city, neighbourhood, or meeting address"
                SearchTab.PEOPLE -> "Search by display name to send a rival request"
            },
            color = TextMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun NoResultsState(query: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp),
    ) {
        Text(text = "😶", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No results for \"$query\"",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Try a different city, neighbourhood, or name",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
