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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import paceup.composeapp.generated.resources.Res
import paceup.composeapp.generated.resources.email_verification_back_to_login
import paceup.composeapp.generated.resources.email_verification_body
import paceup.composeapp.generated.resources.email_verification_title
import paceup.composeapp.generated.resources.ic_paceup_mark

private val BackgroundColor = Color(0xFF0D1B2A)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val ElectricBlue = Color(0xFF4FC3F7)
private val PrimaryBlue = Color(0xFF1A73E8)

/** Stateless screen shown after successful signup — prompts user to check their email. */
@Composable
fun EmailVerificationRoot(
    email: String,
    onBackToLogin: () -> Unit
) {
    EmailVerificationScreen(email = email, onBackToLogin = onBackToLogin)
}

@Composable
fun EmailVerificationScreen(
    email: String,
    onBackToLogin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        PaceGridBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_paceup_mark),
                contentDescription = "PaceUp logo",
                modifier = Modifier.size(52.dp)
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = stringResource(Res.string.email_verification_title),
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = email,
                color = ElectricBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.email_verification_body),
                color = TextMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onBackToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text = stringResource(Res.string.email_verification_back_to_login),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PaceGridBackground(modifier: Modifier = Modifier) {
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
    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceEvenly) {
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
