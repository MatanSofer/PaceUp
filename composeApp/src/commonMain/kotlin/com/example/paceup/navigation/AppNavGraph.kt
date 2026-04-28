package com.example.paceup.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.paceup.feature.welcome.WelcomeRoot

/** Registers all PaceUp destinations. Stub screens replaced per feature task. */
fun NavGraphBuilder.appGraph(navController: NavController) {
    composable<WelcomeRoute> {
        WelcomeRoot(onNavigateToLogin = { navController.navigate(LoginRoute) })
    }
    composable<LoginRoute> {
        StubScreen("Login") { navController.navigate(SignUpRoute) }
    }
    composable<SignUpRoute> {
        StubScreen("Sign Up") { navController.navigate(StravaConnectRoute) }
    }
    composable<StravaConnectRoute> {
        StubScreen("Strava Connect") { navController.navigate(OnboardingLocationRoute) }
    }
    composable<OnboardingLocationRoute> {
        StubScreen("Location Permission") { navController.navigate(OnboardingNotificationsRoute) }
    }
    composable<OnboardingNotificationsRoute> {
        StubScreen("Notification Permission") { navController.navigate(OnboardingProfileRoute) }
    }
    composable<OnboardingProfileRoute> {
        StubScreen("Profile Setup") { navController.navigate(HomeRoute) }
    }
    composable<HomeRoute> {
        StubScreen("Home (Map)")
    }
    composable<RunDetailRoute> { backStackEntry ->
        val route: RunDetailRoute = backStackEntry.toRoute()
        StubScreen("Run Detail — ${route.runId}")
    }
    composable<CreateRunRoute> {
        StubScreen("Create Run")
    }
    composable<UserProfileRoute> { backStackEntry ->
        val route: UserProfileRoute = backStackEntry.toRoute()
        StubScreen("User Profile — ${route.userId}")
    }
    composable<RivalDashboardRoute> {
        StubScreen("Rival Dashboard")
    }
    composable<SettingsRoute> {
        StubScreen("Settings") { navController.navigate(SettingsAccountRoute) }
    }
    composable<SettingsAccountRoute> {
        StubScreen("Settings — Account")
    }
    composable<SettingsNotificationsRoute> {
        StubScreen("Settings — Notifications")
    }
    composable<SettingsPrivacyRoute> {
        StubScreen("Settings — Privacy")
    }
    composable<SettingsAppRoute> {
        StubScreen("Settings — App")
    }
    composable<BlockedUsersRoute> {
        StubScreen("Blocked Users")
    }
}

@Composable
private fun StubScreen(name: String, onTap: (() -> Unit)? = null) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = name)
    }
}
