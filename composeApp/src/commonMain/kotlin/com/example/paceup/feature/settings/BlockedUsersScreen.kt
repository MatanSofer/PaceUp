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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.shared.runmatching.domain.UserSummary
import com.example.paceup.ui.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

private val BackgroundColor = Color(0xFF0D1B2A)
private val SurfaceColor = Color(0xFF1F2937)
private val DividerColor = Color(0xFF374151)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val PrimaryBlue = Color(0xFF1A73E8)
private val ErrorRed = Color(0xFFA32D2D)

// ── Root ──────────────────────────────────────────────────────────────────────

@Composable
fun BlockedUsersRoot(
    onNavigateBack: () -> Unit,
    viewModel: BlockedUsersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            BlockedUsersEvent.NavigateBack -> onNavigateBack()
        }
    }

    BlockedUsersScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    state: BlockedUsersState,
    onAction: (BlockedUsersAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Blocked Users",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onAction(BlockedUsersAction.OnBackClick) },
                    ) {
                        Text(text = "←", color = TextPrimary, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor),
            )
        },
        containerColor = BackgroundColor,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        color = PrimaryBlue,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                state.error != null -> {
                    Text(
                        text = state.error.toString(),
                        color = TextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                    )
                }
                state.blockedUsers.isEmpty() -> {
                    EmptyBlockedState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    BlockedUsersList(
                        users = state.blockedUsers,
                        unblockingId = state.unblockingId,
                        onUnblock = { userId -> onAction(BlockedUsersAction.OnUnblockClick(userId)) },
                    )
                }
            }
        }
    }
}

// ── List ──────────────────────────────────────────────────────────────────────

@Composable
private fun BlockedUsersList(
    users: List<UserSummary>,
    unblockingId: String?,
    onUnblock: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(users, key = { it.id }) { user ->
            BlockedUserRow(
                user = user,
                isUnblocking = user.id == unblockingId,
                onUnblock = { onUnblock(user.id) },
            )
            HorizontalDivider(
                color = DividerColor,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun BlockedUserRow(
    user: UserSummary,
    isUnblocking: Boolean,
    onUnblock: () -> Unit,
) {
    val zoneColor = when (user.paceZone) {
        "A" -> Color(0xFF7C3AED)
        "B" -> PrimaryBlue
        "C" -> Color(0xFF0F6E56)
        "D" -> Color(0xFFBA7517)
        else -> Color(0xFF6B7280)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Avatar initial
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(zoneColor.copy(alpha = 0.15f))
                .border(1.dp, zoneColor.copy(alpha = 0.4f), CircleShape),
        ) {
            Text(
                text = user.displayName.take(1).uppercase(),
                color = zoneColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            user.paceZone?.let { zone ->
                Text(
                    text = "Zone $zone",
                    color = zoneColor,
                    fontSize = 12.sp,
                )
            }
        }

        if (isUnblocking) {
            CircularProgressIndicator(
                color = TextMuted,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                    .clickable { onUnblock() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "Unblock",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyBlockedState(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(40.dp),
    ) {
        Text(text = "🚫", fontSize = 48.sp)
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = "No blocked users",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "Users you block will appear here. You can unblock them at any time.",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}
