package com.example.paceup.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SurfaceColor = Color(0xFF1F2937)
private val DividerColor = Color(0xFF374151)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val PrimaryBlue = Color(0xFF1A73E8)
private val SuccessGreen = Color(0xFF0F6E56)

/**
 * Modal dialog for submitting a report.
 * Local UI state (selected reason, description) lives here via [remember]; the API call
 * is delegated to the parent ViewModel via [onSubmit].
 *
 * Spec §6.2.
 */
@Composable
fun ReportDialog(
    target: ReportTarget,
    isSubmitting: Boolean,
    isSuccess: Boolean,
    onSubmit: (reason: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        containerColor = SurfaceColor,
        titleContentColor = TextPrimary,
        title = {
            Text(
                text = if (isSuccess) "Report Submitted" else target.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            )
        },
        text = {
            if (isSuccess) {
                SuccessContent()
            } else {
                ReportFormContent(
                    reasons = target.reasons,
                    selectedReason = selectedReason,
                    description = description,
                    onSelectReason = { selectedReason = it },
                    onDescriptionChange = { description = it },
                )
            }
        },
        confirmButton = {
            if (isSuccess) {
                TextButton(onClick = onDismiss) {
                    Text("Done", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                }
            } else if (isSubmitting) {
                CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 2.dp)
            } else {
                TextButton(
                    onClick = {
                        val reason = selectedReason ?: return@TextButton
                        onSubmit(reason, description)
                    },
                    enabled = selectedReason != null,
                ) {
                    Text(
                        text = "Submit",
                        color = if (selectedReason != null) PrimaryBlue else TextMuted,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        dismissButton = {
            if (!isSuccess && !isSubmitting) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextMuted)
                }
            }
        },
    )
}

@Composable
private fun ReportFormContent(
    reasons: List<String>,
    selectedReason: String?,
    description: String,
    onSelectReason: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    Column {
        Text(
            text = "Select a reason:",
            color = TextMuted,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(10.dp))

        reasons.forEach { reason ->
            val isSelected = reason == selectedReason
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) PrimaryBlue.copy(alpha = 0.6f) else DividerColor,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelectReason(reason) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = reason,
                    color = if (isSelected) PrimaryBlue else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = { Text("Additional details (optional)", color = TextMuted, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = DividerColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryBlue,
            ),
        )
    }
}

@Composable
private fun SuccessContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(text = "✓", color = SuccessGreen, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Thank you. We'll review this within 24 hours.",
            color = TextMuted,
            fontSize = 14.sp,
        )
    }
}
