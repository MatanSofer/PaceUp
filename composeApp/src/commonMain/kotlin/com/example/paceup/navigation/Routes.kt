package com.example.paceup.navigation

import kotlinx.serialization.Serializable

// Onboarding
@Serializable data object WelcomeRoute
@Serializable data object LoginRoute
@Serializable data object SignUpRoute
@Serializable data class EmailVerificationRoute(val email: String)
@Serializable data object StravaConnectRoute
@Serializable data object OnboardingLocationRoute
@Serializable data object OnboardingNotificationsRoute
@Serializable data object OnboardingProfileRoute

// Main
@Serializable data object HomeRoute
@Serializable data class RunDetailRoute(val runId: String)
@Serializable data object CreateRunRoute
@Serializable data class UserProfileRoute(val userId: String)
@Serializable data object RivalDashboardRoute

// Settings
@Serializable data object SettingsRoute
@Serializable data object SettingsAccountRoute
@Serializable data object SettingsNotificationsRoute
@Serializable data object SettingsPrivacyRoute
@Serializable data object SettingsAppRoute
@Serializable data object BlockedUsersRoute
