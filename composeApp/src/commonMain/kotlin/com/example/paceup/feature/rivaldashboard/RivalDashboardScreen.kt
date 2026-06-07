package com.example.paceup.feature.rivaldashboard

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.feature.home.ZoneBadge
import com.example.paceup.feature.home.color
import com.example.paceup.shared.pacezone.PaceZone
import com.example.paceup.shared.runmatching.domain.UserSummary
import com.example.paceup.ui.ObserveAsEvents
import com.example.paceup.ui.asString
import org.koin.compose.viewmodel.koinViewModel

// ─── Colors ──────────────────────────────────────────────────────────────────

private val Background   = Color(0xFF0D1B2A)
private val Surface      = Color(0xFF1A2E3E)
private val SurfaceAlt   = Color(0xFF1F3448)
private val PrimaryBlue  = Color(0xFF1D6FA8)
private val AccentOrange = Color(0xFFFC4C02)
private val TextPrimary  = Color(0xFFE8EAF0)
private val TextSecondary = Color(0xFF8892A4)
private val SuccessGreen = Color(0xFF0F6E56)

/** Converts a zone letter string ("A"–"E") to its display color. */
private fun zoneStringToColor(zone: String?): Color =
    PaceZone.entries.find { it.name == zone?.uppercase() }?.color() ?: Color(0xFF6B7280)

// ─── Root composable ─────────────────────────────────────────────────────────

@Composable
fun RivalDashboardRoot(
    onNavigateBack: () -> Unit,
    viewModel: RivalDashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            RivalDashboardEvent.NavigateBack -> onNavigateBack()
        }
    }
    RivalDashboardScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RivalDashboardScreen(
    state: RivalDashboardState,
    onAction: (RivalDashboardAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rivals",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onAction(RivalDashboardAction.OnBackClick) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("←", color = TextPrimary, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(RivalDashboardAction.OnAddRivalClick) },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Light)
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding(),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryBlue,
                    )
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(state.error.asString(), color = TextSecondary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onAction(RivalDashboardAction.OnRefresh) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        ) { Text("Retry") }
                    }
                }
                else -> {
                    RivalDashboardContent(state = state, onAction = onAction)
                }
            }
        }
    }

    if (state.showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { onAction(RivalDashboardAction.OnDismissAddSheet) },
            sheetState = sheetState,
            containerColor = Surface,
        ) {
            AddRivalSheetContent(state = state, onAction = onAction)
        }
    }
}

// ─── Main content ─────────────────────────────────────────────────────────────

@Composable
private fun RivalDashboardContent(
    state: RivalDashboardState,
    onAction: (RivalDashboardAction) -> Unit,
) {
    val isEmpty = state.activeRivals.isEmpty() &&
        state.incomingRequests.isEmpty() &&
        state.outgoingRequests.isEmpty()

    if (isEmpty) {
        RivalEmptyState(onAddClick = { onAction(RivalDashboardAction.OnAddRivalClick) })
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.incomingRequests.isNotEmpty()) {
            item { SectionHeader("Rival Requests") }
            items(state.incomingRequests, key = { it.rival.id }) { details ->
                IncomingRequestCard(details = details, onAction = onAction)
            }
        }

        if (state.outgoingRequests.isNotEmpty()) {
            item { SectionHeader("Sent Requests") }
            items(state.outgoingRequests, key = { it.rival.id }) { details ->
                OutgoingRequestCard(details = details)
            }
        }

        if (state.activeRivals.isNotEmpty()) {
            item { SectionHeader("My Rivals") }
            items(state.activeRivals, key = { it.rival.id }) { details ->
                RivalScoreboardCard(
                    details = details,
                    currentUserId = state.currentUserId,
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─── Scoreboard card ─────────────────────────────────────────────────────────

@Composable
private fun RivalScoreboardCard(
    details: RivalWithDetails,
    currentUserId: String,
) {
    val rival    = details.rival
    val snapshot = details.snapshot
    val opponent = details.opponentSummary

    val myKm       = if (rival.userAId == currentUserId) snapshot?.userAKm else snapshot?.userBKm
    val opKm       = if (rival.userAId == currentUserId) snapshot?.userBKm else snapshot?.userAKm
    val myRuns     = if (rival.userAId == currentUserId) snapshot?.userARuns else snapshot?.userBRuns
    val opRuns     = if (rival.userAId == currentUserId) snapshot?.userBRuns else snapshot?.userARuns
    val myBestPace = if (rival.userAId == currentUserId) snapshot?.userABestPace else snapshot?.userBBestPace
    val opBestPace = if (rival.userAId == currentUserId) snapshot?.userBBestPace else snapshot?.userABestPace
    val myWins     = if (rival.userAId == currentUserId) rival.userAWins else rival.userBWins
    val opWins     = if (rival.userAId == currentUserId) rival.userBWins else rival.userAWins

    val totalKm     = (myKm ?: 0f) + (opKm ?: 0f)
    val myFraction  = if (totalKm > 0f) (myKm ?: 0f) / totalKm else 0.5f
    val iAmLeading  = (myKm ?: 0f) > (opKm ?: 0f)

    Surface(
        color = Surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerColumn(
                    name = "You",
                    paceZone = null,
                    weeklyKm = myKm,
                    isLeading = iAmLeading,
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("VS", color = TextSecondary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                PlayerColumn(
                    name = opponent?.displayName ?: "Rival",
                    paceZone = opponent?.paceZone,
                    weeklyKm = opKm,
                    isLeading = !iAmLeading && (opKm ?: 0f) != (myKm ?: 0f),
                    modifier = Modifier.weight(1f),
                    alignEnd = true,
                )
            }

            Spacer(Modifier.height(12.dp))

            Column {
                Text("This week", color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { myFraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (iAmLeading) SuccessGreen else AccentOrange,
                    trackColor = SurfaceAlt,
                    strokeCap = StrokeCap.Round,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatCell("Runs", "${myRuns ?: 0}", "${opRuns ?: 0}")
                VerticalSeparator()
                StatCell("Best pace", formatPaceSec(myBestPace), formatPaceSec(opBestPace))
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = SurfaceAlt)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Record  $myWins – $opWins", color = TextSecondary, fontSize = 12.sp)

                if (rival.streakCount >= 3) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("🔥", fontSize = 16.sp)
                        Text(
                            text = "${rival.streakCount}-week streak",
                            color = AccentOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerColumn(
    name: String,
    paceZone: String?,
    weeklyKm: Float?,
    isLeading: Boolean,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    val align = if (alignEnd) Alignment.End else Alignment.Start
    Column(modifier = modifier, horizontalAlignment = align) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isLeading) PrimaryBlue.copy(alpha = 0.3f) else SurfaceAlt)
                .then(if (isLeading) Modifier.border(2.dp, PrimaryBlue, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1).uppercase(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        if (paceZone != null) {
            Spacer(Modifier.height(2.dp))
            ZoneBadge(zone = paceZone, paceRange = "", color = zoneStringToColor(paceZone))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = weeklyKm?.let { "%.1f km".format(it) } ?: "— km",
            color = if (isLeading) TextPrimary else TextSecondary,
            fontWeight = if (isLeading) FontWeight.Bold else FontWeight.Normal,
            fontSize = 22.sp,
        )
    }
}

@Composable
private fun StatCell(label: String, myValue: String, opValue: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text("$myValue / $opValue", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun VerticalSeparator() {
    Box(modifier = Modifier.width(1.dp).height(36.dp).background(SurfaceAlt))
}

private fun formatPaceSec(paceSecPerKm: Int?): String {
    if (paceSecPerKm == null) return "—"
    val m = paceSecPerKm / 60
    val s = paceSecPerKm % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

// ─── Request cards ────────────────────────────────────────────────────────────

@Composable
private fun IncomingRequestCard(
    details: RivalWithDetails,
    onAction: (RivalDashboardAction) -> Unit,
) {
    Surface(
        color = Surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = details.opponentSummary?.displayName ?: "Runner",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                details.opponentSummary?.paceZone?.let { zone ->
                    Spacer(Modifier.height(4.dp))
                    ZoneBadge(zone = zone, paceRange = "", color = zoneStringToColor(zone))
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { onAction(RivalDashboardAction.OnDeclineRival(details.rival.id)) },
                border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary),
            ) {
                Text("Decline", color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onAction(RivalDashboardAction.OnAcceptRival(details.rival.id)) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text("Accept", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun OutgoingRequestCard(details: RivalWithDetails) {
    Surface(
        color = Surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = details.opponentSummary?.displayName ?: "Runner",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text("Request sent", color = TextSecondary, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(SurfaceAlt)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("Pending", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

// ─── Add Rival bottom sheet ───────────────────────────────────────────────────

@Composable
private fun AddRivalSheetContent(
    state: RivalDashboardState,
    onAction: (RivalDashboardAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text("Find a Rival", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.addSearchQuery,
            onValueChange = { onAction(RivalDashboardAction.OnAddSearchQueryChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name…", color = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = SurfaceAlt,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryBlue,
            ),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
        )

        Spacer(Modifier.height(12.dp))

        if (state.isSearching) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = PrimaryBlue)
        } else if (state.addSearchQuery.length >= 2 && state.addSearchResults.isEmpty()) {
            Text("No runners found", color = TextSecondary, fontSize = 13.sp)
        }

        if (state.addError != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.addError.asString(), color = Color(0xFFA32D2D), fontSize = 13.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.addSearchResults.forEach { user ->
                SearchResultRow(user = user, onSend = { onAction(RivalDashboardAction.OnSendRivalRequest(user.id)) })
            }
        }
    }
}

@Composable
private fun SearchResultRow(user: UserSummary, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceAlt)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            user.paceZone?.let { zone ->
                Spacer(Modifier.height(2.dp))
                ZoneBadge(zone = zone, paceRange = "", color = zoneStringToColor(zone))
            }
        }
        FilledTonalButton(
            onClick = onSend,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = PrimaryBlue),
        ) {
            Text("Challenge", color = Color.White, fontSize = 12.sp)
        }
    }
}

// ─── Section header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun RivalEmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🏃", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No rivals yet",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Challenge a fellow runner to a weekly distance battle.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Find a Rival", fontWeight = FontWeight.Bold)
        }
    }
}
