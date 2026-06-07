package com.example.paceup.feature.runchat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.shared.runmatching.domain.ChatMessage
import com.example.paceup.ui.ObserveAsEvents
import com.example.paceup.ui.asString
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val BackgroundColor = Color(0xFF0D1B2A)
private val SurfaceColor = Color(0xFF1F2937)
private val SurfaceElevated = Color(0xFF374151)
private val DividerColor = Color(0xFF374151)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val PrimaryBlue = Color(0xFF1A73E8)
private val MyMessageBg = Color(0xFF1D4ED8)
private val OtherMessageBg = SurfaceElevated

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun RunChatRoot(
    runId: String,
    runTitle: String,
    onNavigateBack: () -> Unit,
    viewModel: RunChatViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            RunChatEvent.NavigateBack -> onNavigateBack()
        }
    }

    RunChatScreen(
        title = runTitle,
        state = state,
        onAction = viewModel::onAction,
    )
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun RunChatScreen(
    title: String,
    state: RunChatState,
    onAction: (RunChatAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .imePadding(),
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        ChatTopBar(
            title = title,
            onBack = { onAction(RunChatAction.OnBackClick) },
        )

        // ── Offline banner ───────────────────────────────────────────────────
        if (state.isOffline) {
            OfflineBanner()
        }

        // ── Error banner ──────────────────────────────────────────────────────
        state.error?.let { err ->
            ErrorBanner(
                message = err.asString(),
                onDismiss = { onAction(RunChatAction.OnDismissError) },
            )
        }

        // ── Message list ─────────────────────────────────────────────────────
        if (state.isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(32.dp))
            }
        } else {
            MessageList(
                messages = state.messages,
                currentUserId = state.currentUserId,
                onLongPressMessage = { onAction(RunChatAction.OnLongPressMessage(it)) },
                modifier = Modifier.weight(1f),
            )
        }

        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

        // ── Input bar ────────────────────────────────────────────────────────
        ChatInputBar(
            input = state.input,
            isSending = state.isSending,
            onInputChange = { onAction(RunChatAction.OnInputChange(it)) },
            onSend = { onAction(RunChatAction.OnSendClick) },
        )
    }

    // Report dialog — triggered by long-press on another user's message
    val reportTarget = state.reportTarget
    if (reportTarget != null) {
        com.example.paceup.feature.report.ReportDialog(
            target = reportTarget,
            isSubmitting = state.isReportSubmitting,
            isSuccess = state.isReportSuccess,
            onSubmit = { reason, desc -> onAction(RunChatAction.OnSubmitReport(reason, desc)) },
            onDismiss = { onAction(RunChatAction.OnDismissReport) },
        )
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun ChatTopBar(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceElevated)
                .border(1.dp, DividerColor, CircleShape)
                .clickable { onBack() },
        ) {
            Text(text = "←", color = TextPrimary, fontSize = 16.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = "💬  Group Chat",
                color = TextMuted,
                fontSize = 11.sp,
                letterSpacing = 0.4.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ── Banners ───────────────────────────────────────────────────────────────────

@Composable
private fun OfflineBanner() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFBA7517))
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = "You're offline — new messages will appear when reconnected",
            color = Color.White,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFA32D2D))
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(text = message, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            text = "✕",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .clickable { onDismiss() }
                .padding(8.dp),
        )
    }
}

// ── Message list ──────────────────────────────────────────────────────────────

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    currentUserId: String?,
    onLongPressMessage: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (messages.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "💬", fontSize = 36.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No messages yet",
                    color = TextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(text = "Be the first to say hi!", color = TextMuted, fontSize = 13.sp)
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            val isMe = message.userId == currentUserId
            MessageBubble(
                message = message,
                isMe = isMe,
                onLongPress = if (!isMe) ({ onLongPressMessage(message) }) else null,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(message: ChatMessage, isMe: Boolean, onLongPress: (() -> Unit)?) {
    Column(
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (!isMe) {
            Text(
                text = message.senderName,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp),
            )
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp,
                    )
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress,
                )
                .background(if (isMe) MyMessageBg else OtherMessageBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = message.content,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

// ── Input bar ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    input: String,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceElevated)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (input.isEmpty()) {
                Text("Message…", color = TextMuted, fontSize = 14.sp)
            }
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = TextPrimary,
                    fontSize = 14.sp,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.width(8.dp))

        // Send button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (input.isBlank() || isSending) SurfaceElevated else PrimaryBlue)
                .then(
                    if (!input.isBlank() && !isSending)
                        Modifier.clickable { onSend() }
                    else Modifier
                ),
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    color = TextMuted,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(
                    text = "↑",
                    color = if (input.isBlank()) TextMuted else Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
