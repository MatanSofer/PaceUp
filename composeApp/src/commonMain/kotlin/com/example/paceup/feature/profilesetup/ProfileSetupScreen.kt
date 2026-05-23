package com.example.paceup.feature.profilesetup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.paceup.ui.ObserveAsEvents
import com.example.paceup.ui.asString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import paceup.composeapp.generated.resources.Res
import paceup.composeapp.generated.resources.profile_avatar_hint
import paceup.composeapp.generated.resources.profile_continue_button
import paceup.composeapp.generated.resources.profile_display_name_hint
import paceup.composeapp.generated.resources.profile_display_name_label
import paceup.composeapp.generated.resources.profile_subtitle
import paceup.composeapp.generated.resources.profile_title

private val BackgroundColor = Color(0xFF0D1B2A)
private val SurfaceColor = Color(0xFF1F2937)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val TextFaint = Color(0xFF4B5563)
private val PrimaryBlue = Color(0xFF1A73E8)
private val ErrorRed = Color(0xFFEF4444)

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun ProfileSetupRoot(
    onNavigateToHome: () -> Unit,
    viewModel: ProfileSetupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ProfileSetupEvent.NavigateToHome -> onNavigateToHome()
        }
    }

    ProfileSetupScreen(state = state, onAction = viewModel::onAction)
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ProfileSetupScreen(
    state: ProfileSetupState,
    onAction: (ProfileSetupAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Text(
                text = stringResource(Res.string.profile_title),
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.profile_subtitle),
                color = TextMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(32.dp))

            // Avatar picker
            AvatarPicker(
                avatarBytes = state.avatarBytes,
                onClick = { onAction(ProfileSetupAction.OnPickAvatarClicked) }
            )

            Spacer(Modifier.height(32.dp))

            // Display name field
            OutlinedTextField(
                value = state.displayName,
                onValueChange = { onAction(ProfileSetupAction.OnDisplayNameChanged(it)) },
                label = { Text(stringResource(Res.string.profile_display_name_label)) },
                placeholder = { Text(stringResource(Res.string.profile_display_name_hint)) },
                singleLine = true,
                isError = state.displayNameError != null,
                supportingText = state.displayNameError?.let {
                    { Text(it.asString(), color = ErrorRed, fontSize = 12.sp) }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onAction(ProfileSetupAction.OnContinueClicked) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_display_name_field"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color(0xFF374151),
                    errorBorderColor = ErrorRed,
                    focusedLabelColor = PrimaryBlue,
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = PrimaryBlue,
                )
            )

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.error.asString(),
                    color = ErrorRed,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onAction(ProfileSetupAction.OnContinueClicked) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("profile_continue_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = TextPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.profile_continue_button),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

// ── Avatar Picker ─────────────────────────────────────────────────────────────

@Composable
private fun AvatarPicker(
    avatarBytes: ByteArray?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(SurfaceColor)
            .border(2.dp, Color(0xFF374151), CircleShape)
            .clickable(onClick = onClick)
            .testTag("profile_avatar_picker"),
        contentAlignment = Alignment.Center
    ) {
        if (avatarBytes != null) {
            AsyncImage(
                model = avatarBytes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Dim overlay to hint re-tap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
        // Camera icon label
        Text(
            text = if (avatarBytes == null) "📷" else "✎",
            fontSize = if (avatarBytes == null) 32.sp else 20.sp,
            color = if (avatarBytes == null) TextMuted else TextPrimary
        )
    }

    Spacer(Modifier.height(8.dp))

    Text(
        text = stringResource(Res.string.profile_avatar_hint),
        color = TextFaint,
        fontSize = 12.sp
    )
}
