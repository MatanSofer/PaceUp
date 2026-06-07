package com.example.paceup.feature.report

/** What is being reported — determines the reason list shown in [ReportDialog]. Spec §6.2. */
sealed class ReportTarget {
    data class User(val userId: String, val displayName: String) : ReportTarget()
    data class Run(val runId: String, val runTitle: String) : ReportTarget()
    /** For message reports, the sender's userId is stored; message content goes into description. */
    data class Message(val senderId: String, val senderName: String, val messageContent: String) : ReportTarget()
}

/** Reason options per report type, matching the spec §6.2 reason table. */
val ReportTarget.reasons: List<String>
    get() = when (this) {
        is ReportTarget.User -> listOf(
            "Fake pace / misleading profile",
            "Inappropriate behavior",
            "Fake identity",
            "Other",
        )
        is ReportTarget.Run -> listOf(
            "Misleading description",
            "Inappropriate content",
            "Scam / fake run",
            "Other",
        )
        is ReportTarget.Message -> listOf(
            "Harassment",
            "Spam",
            "Other",
        )
    }

val ReportTarget.reportType: String
    get() = when (this) {
        is ReportTarget.User -> "user"
        is ReportTarget.Run -> "run"
        is ReportTarget.Message -> "message"
    }

val ReportTarget.title: String
    get() = when (this) {
        is ReportTarget.User -> "Report $displayName"
        is ReportTarget.Run -> "Report Run"
        is ReportTarget.Message -> "Report Message"
    }
