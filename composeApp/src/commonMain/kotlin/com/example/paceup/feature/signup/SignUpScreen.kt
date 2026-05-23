package com.example.paceup.feature.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.ui.ObserveAsEvents
import com.example.paceup.ui.asString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import paceup.composeapp.generated.resources.Res
import paceup.composeapp.generated.resources.ic_apple
import paceup.composeapp.generated.resources.ic_google
import paceup.composeapp.generated.resources.ic_paceup_mark
import paceup.composeapp.generated.resources.signup_apple_button
import paceup.composeapp.generated.resources.signup_button
import paceup.composeapp.generated.resources.signup_confirm_password_hint
import paceup.composeapp.generated.resources.signup_divider_or
import paceup.composeapp.generated.resources.signup_email_hint
import paceup.composeapp.generated.resources.signup_google_button
import paceup.composeapp.generated.resources.signup_have_account
import paceup.composeapp.generated.resources.signup_login_link
import paceup.composeapp.generated.resources.signup_password_hint
import paceup.composeapp.generated.resources.signup_tagline
import paceup.composeapp.generated.resources.signup_title

private val BackgroundColor = Color(0xFF0D1B2A)
private val SurfaceColor = Color(0xFF1F2937)
private val BorderDefault = Color(0xFF374151)
private val BorderFocused = Color(0xFF1A73E8)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val PrimaryBlue = Color(0xFF1A73E8)
private val ErrorRed = Color(0xFFEF4444)

@Composable
fun SignUpRoot(
    onNavigateToEmailVerification: (email: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SignUpViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SignUpEvent.NavigateToEmailVerification -> onNavigateToEmailVerification(event.email)
            SignUpEvent.NavigateToLogin -> onNavigateToLogin()
        }
    }

    SignUpScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun SignUpScreen(
    state: SignUpState,
    onAction: (SignUpAction) -> Unit
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        PaceNumberGridBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            Image(
                painter = painterResource(Res.drawable.ic_paceup_mark),
                contentDescription = "PaceUp logo",
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.signup_title),
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.signup_tagline),
                color = TextMuted,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(36.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = { onAction(SignUpAction.OnEmailChanged(it)) },
                label = { Text(stringResource(Res.string.signup_email_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_email_field"),
                shape = RoundedCornerShape(10.dp),
                colors = signUpTextFieldColors()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = { onAction(SignUpAction.OnPasswordChanged(it)) },
                label = { Text(stringResource(Res.string.signup_password_hint)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "Hide" else "Show",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_password_field"),
                shape = RoundedCornerShape(10.dp),
                colors = signUpTextFieldColors()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = { onAction(SignUpAction.OnConfirmPasswordChanged(it)) },
                label = { Text(stringResource(Res.string.signup_confirm_password_hint)) },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onAction(SignUpAction.OnSignUpClicked) }
                ),
                trailingIcon = {
                    TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Text(
                            text = if (confirmPasswordVisible) "Hide" else "Show",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_confirm_password_field"),
                shape = RoundedCornerShape(10.dp),
                colors = signUpTextFieldColors()
            )

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.error.asString(),
                    color = ErrorRed,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_error_text")
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onAction(SignUpAction.OnSignUpClicked) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("signup_create_account_button"),
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
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.signup_button),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = BorderDefault
                )
                Text(
                    text = stringResource(Res.string.signup_divider_or),
                    color = TextMuted,
                    fontSize = 13.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = BorderDefault
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { onAction(SignUpAction.OnGoogleSignInClicked) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("signup_google_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDefault),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary,
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = androidx.compose.ui.graphics.Color.Unspecified
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(Res.string.signup_google_button),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { onAction(SignUpAction.OnAppleSignInClicked) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("signup_apple_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDefault),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary,
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_apple),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TextPrimary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(Res.string.signup_apple_button),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(Res.string.signup_have_account),
                    color = TextMuted,
                    fontSize = 14.sp
                )
                TextButton(onClick = { onAction(SignUpAction.OnLoginClicked) }) {
                    Text(
                        text = stringResource(Res.string.signup_login_link),
                        color = PrimaryBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun signUpTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceColor,
    unfocusedContainerColor = SurfaceColor,
    focusedBorderColor = BorderFocused,
    unfocusedBorderColor = BorderDefault,
    focusedLabelColor = Color(0xFF4FC3F7),
    unfocusedLabelColor = TextMuted,
    cursorColor = PrimaryBlue,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    errorBorderColor = ErrorRed,
    errorLabelColor = ErrorRed,
    errorTextColor = TextPrimary
)

@Composable
private fun PaceNumberGridBackground(modifier: Modifier = Modifier) {
    val rows = remember {
        listOf(
            listOf("4:22", "4:45", "5:03", "5:31", "6:02"),
            listOf("5:15", "4:33", "5:44", "6:12", "4:19"),
            listOf("4:52", "6:05", "5:07", "4:41", "5:22"),
            listOf("6:18", "4:28", "5:36", "4:44", "6:01"),
            listOf("4:57", "5:25", "6:08", "4:31", "5:48"),
            listOf("5:39", "4:23", "6:14", "5:02", "4:47"),
            listOf("4:18", "5:50", "4:36", "6:22", "5:11"),
            listOf("6:03", "4:54", "5:29", "4:12", "5:41"),
        )
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { pace ->
                    Text(
                        text = pace,
                        color = Color(0xFF4FC3F7).copy(alpha = 0.06f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
