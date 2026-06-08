package com.example.paceup.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.ui.ObserveAsEvents
import com.example.paceup.ui.asString
import org.koin.compose.viewmodel.koinViewModel

// ── Palette (matches Settings dark theme) ─────────────────────────────────────

private val BgColor      = Color(0xFF0D1B2A)
private val Surface      = Color(0xFF1F2937)
private val SurfaceEl    = Color(0xFF374151)
private val Divider      = Color(0xFF374151)
private val TextPri      = Color(0xFFF9FAFB)
private val TextMut      = Color(0xFF9CA3AF)
private val AccentBlue   = Color(0xFF1D6FA8)
private val AccentOrange = Color(0xFFFC4C02)
private val DangerRed    = Color(0xFFA32D2D)
private val SuccessGreen = Color(0xFF0F6E56)

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun AccountSettingsRoot(
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    onShareJson: (String) -> Unit,
    viewModel: AccountSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AccountSettingsEvent.NavigateBack    -> onNavigateBack()
            AccountSettingsEvent.AccountDeleted  -> onAccountDeleted()
            is AccountSettingsEvent.ShareJson    -> onShareJson(event.json)
        }
    }

    AccountSettingsScreen(state = state, onAction = viewModel::onAction)
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun AccountSettingsScreen(
    state: AccountSettingsState,
    onAction: (AccountSettingsAction) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
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
                        .clickable { onAction(AccountSettingsAction.OnBackClick) }
                        .padding(4.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Account",
                    color = TextPri,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            HorizontalDivider(color = Divider, thickness = 0.5.dp)

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                Spacer(Modifier.height(20.dp))

                // Profile
                SectionCard(title = "Profile", modifier = Modifier.padding(horizontal = 16.dp)) {
                    AccountTextField(
                        label = "Display name",
                        value = state.editName,
                        onValueChange = { onAction(AccountSettingsAction.OnNameChange(it)) },
                    )
                    Spacer(Modifier.height(12.dp))
                    AccountTextField(
                        label = "Bio",
                        value = state.editBio,
                        onValueChange = { onAction(AccountSettingsAction.OnBioChange(it)) },
                        singleLine = false,
                        minLines = 3,
                    )
                    Spacer(Modifier.height(14.dp))
                    if (state.profileSaveSuccess) {
                        Text(
                            text = "Profile updated",
                            color = SuccessGreen,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { onAction(AccountSettingsAction.OnDismissProfileSuccess) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    PrimaryButton(
                        text = if (state.isSavingProfile) "Saving…" else "Save profile",
                        enabled = !state.isSavingProfile && state.editName.isNotBlank(),
                        onClick = { onAction(AccountSettingsAction.OnSaveProfile) },
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Email change
                SectionCard(title = "Change email", modifier = Modifier.padding(horizontal = 16.dp)) {
                    AccountTextField(
                        label = "New email address",
                        value = state.newEmail,
                        onValueChange = { onAction(AccountSettingsAction.OnNewEmailChange(it)) },
                    )
                    state.emailError?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(text = it.asString(), color = DangerRed, fontSize = 12.sp)
                    }
                    if (state.emailChangeSuccess) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Confirmation sent — check your inbox",
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable { onAction(AccountSettingsAction.OnDismissEmailResult) },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(
                        text = if (state.isSavingEmail) "Saving…" else "Update email",
                        enabled = !state.isSavingEmail && state.newEmail.isNotBlank(),
                        onClick = { onAction(AccountSettingsAction.OnSaveEmail) },
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Password change
                SectionCard(title = "Change password", modifier = Modifier.padding(horizontal = 16.dp)) {
                    AccountTextField(
                        label = "New password",
                        value = state.newPassword,
                        onValueChange = { onAction(AccountSettingsAction.OnNewPasswordChange(it)) },
                        isPassword = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    AccountTextField(
                        label = "Confirm password",
                        value = state.confirmPassword,
                        onValueChange = { onAction(AccountSettingsAction.OnConfirmPasswordChange(it)) },
                        isPassword = true,
                    )
                    state.passwordError?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(text = it.asString(), color = DangerRed, fontSize = 12.sp)
                    }
                    if (state.passwordChangeSuccess) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Password updated",
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable { onAction(AccountSettingsAction.OnDismissPasswordResult) },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(
                        text = if (state.isSavingPassword) "Saving…" else "Update password",
                        enabled = !state.isSavingPassword && state.newPassword.isNotBlank(),
                        onClick = { onAction(AccountSettingsAction.OnSavePassword) },
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Connected apps
                SectionCard(title = "Connected apps", modifier = Modifier.padding(horizontal = 16.dp)) {
                    ConnectedAppRow(
                        name = "Strava",
                        connected = state.profile?.stravaConnected == true,
                        isBusy = state.isDisconnectingStrava,
                        accentColor = AccentOrange,
                        onDisconnect = { onAction(AccountSettingsAction.OnDisconnectStrava) },
                    )
                    HorizontalDivider(
                        color = Divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                    ConnectedAppRow(
                        name = "Garmin",
                        connected = state.profile?.garminConnected == true,
                        isBusy = state.isDisconnectingGarmin,
                        accentColor = AccentBlue,
                        onDisconnect = { onAction(AccountSettingsAction.OnDisconnectGarmin) },
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Danger zone
                SectionCard(title = "Danger zone", modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedActionRow(
                        label = "Export my data",
                        sublabel = "Download all your PaceUp data (GDPR)",
                        isBusy = state.isExportingData,
                        borderColor = AccentBlue,
                        onClick = { onAction(AccountSettingsAction.OnExportDataClick) },
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedActionRow(
                        label = "Delete account",
                        sublabel = "Permanently delete your account and data",
                        isBusy = state.isDeletingAccount,
                        borderColor = DangerRed,
                        onClick = { onAction(AccountSettingsAction.OnDeleteAccountClick) },
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }

        // Delete confirmation dialog
        if (state.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { onAction(AccountSettingsAction.OnDismissDeleteDialog) },
                containerColor = Surface,
                titleContentColor = TextPri,
                textContentColor = TextMut,
                title = { Text("Delete account", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "This action is permanent and cannot be undone. All your runs, ratings, and data will be deleted.",
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Type DELETE to confirm",
                            color = DangerRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(8.dp))
                        AccountTextField(
                            label = "Type DELETE",
                            value = state.deleteConfirmText,
                            onValueChange = { onAction(AccountSettingsAction.OnDeleteConfirmTextChange(it)) },
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onAction(AccountSettingsAction.OnConfirmDeleteAccount) },
                        enabled = state.deleteConfirmText == "DELETE" && !state.isDeletingAccount,
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    ) {
                        Text(if (state.isDeletingAccount) "Deleting…" else "Delete forever")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(AccountSettingsAction.OnDismissDeleteDialog) }) {
                        Text("Cancel", color = TextMut)
                    }
                },
            )
        }
    }
}

// ── Private components ────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
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
                .padding(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun AccountTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextMut, fontSize = 13.sp) },
        singleLine = singleLine,
        minLines = minLines,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPri,
            unfocusedTextColor = TextPri,
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = Divider,
            cursorColor = AccentBlue,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ConnectedAppRow(
    name: String,
    connected: Boolean,
    isBusy: Boolean,
    accentColor: Color,
    onDisconnect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                text = if (connected) "Connected" else "Not connected",
                color = if (connected) SuccessGreen else TextMut,
                fontSize = 12.sp,
            )
        }
        if (connected) {
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accentColor, strokeWidth = 2.dp)
            } else {
                Text(
                    text = "Disconnect",
                    color = DangerRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDisconnect)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .border(0.5.dp, DangerRed, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun OutlinedActionRow(
    label: String,
    sublabel: String,
    isBusy: Boolean,
    borderColor: Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = !isBusy, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = borderColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = sublabel, color = TextMut, fontSize = 12.sp)
        }
        if (isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = borderColor, strokeWidth = 2.dp)
        }
    }
}
