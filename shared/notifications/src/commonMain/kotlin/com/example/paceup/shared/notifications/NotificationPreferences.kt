package com.example.paceup.shared.notifications

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** User's push notification opt-in state for each category (spec §5.2). */
data class NotificationPreferences(
    val runReminders: Boolean = true,
    val joinRequests: Boolean = true,
    val rivalNudges: Boolean = true,
    val rivalSummary: Boolean = true,
    val newRuns: Boolean = true,
    val partnerRatings: Boolean = true,
    val marketing: Boolean = true,
)

/** DTO for reading/writing notification preference columns on the users table. */
@Serializable
internal data class NotificationPreferencesDto(
    @SerialName("notif_run_reminders")  val runReminders: Boolean  = true,
    @SerialName("notif_join_requests")  val joinRequests: Boolean  = true,
    @SerialName("notif_rival_nudges")   val rivalNudges: Boolean   = true,
    @SerialName("notif_rival_summary")  val rivalSummary: Boolean  = true,
    @SerialName("notif_new_runs")       val newRuns: Boolean       = true,
    @SerialName("notif_partner_ratings") val partnerRatings: Boolean = true,
    @SerialName("notif_marketing")      val marketing: Boolean     = true,
) {
    fun toDomain() = NotificationPreferences(
        runReminders   = runReminders,
        joinRequests   = joinRequests,
        rivalNudges    = rivalNudges,
        rivalSummary   = rivalSummary,
        newRuns        = newRuns,
        partnerRatings = partnerRatings,
        marketing      = marketing,
    )
}
