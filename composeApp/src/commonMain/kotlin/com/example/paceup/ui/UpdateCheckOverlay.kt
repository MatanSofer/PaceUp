package com.example.paceup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import paceup.composeapp.generated.resources.Res
import paceup.composeapp.generated.resources.version_force_update_body
import paceup.composeapp.generated.resources.version_force_update_title
import paceup.composeapp.generated.resources.version_soft_update_body
import paceup.composeapp.generated.resources.version_update_button

private val SurfaceColor = Color(0xFF1F2937)
private val PrimaryBlue = Color(0xFF1A73E8)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val ElectricBlue = Color(0xFF4FC3F7)

/** Non-dismissible dialog shown when force_update = true and version is below minimum. */
@Composable
fun ForceUpdateDialog(message: String?, onOpenStore: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* intentionally non-dismissible */ },
        containerColor = SurfaceColor,
        title = {
            Text(
                text = stringResource(Res.string.version_force_update_title),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = message ?: stringResource(Res.string.version_force_update_body),
                color = TextMuted,
                fontSize = 15.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onOpenStore,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.version_update_button),
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

/** Dismissible banner shown when force_update = false and version is below minimum. */
@Composable
fun SoftUpdateBanner(
    message: String?,
    onDismiss: () -> Unit,
    onOpenStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D2A4A))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = message ?: stringResource(Res.string.version_soft_update_body),
            color = TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onOpenStore) {
            Text(
                text = stringResource(Res.string.version_update_button),
                color = ElectricBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Text(text = "×", color = TextMuted, fontSize = 18.sp)
        }
    }
}
