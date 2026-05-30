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
import com.example.paceup.feature.createrun.CreateRunRoot
import com.example.paceup.feature.home.HomeRoot
import com.example.paceup.feature.rundetail.RunDetailRoot
import com.example.paceup.feature.search.SearchRoot
import com.example.paceup.feature.login.LoginRoot
import com.example.paceup.feature.signup.EmailVerificationRoot
import com.example.paceup.feature.signup.SignUpRoot
import com.example.paceup.feature.locationpermission.LocationPermissionRoot
import com.example.paceup.feature.notificationpermission.NotificationPermissionRoot
import com.example.paceup.feature.profilesetup.ProfileSetupRoot
import com.example.paceup.feature.stravaconnect.StravaConnectRoot
import com.example.paceup.feature.welcome.WelcomeRoot

/** Registers all PaceUp destinations. Stub screens replaced per feature task. */
fun NavGraphBuilder.appGraph(navController: NavController) {
    composable<WelcomeRoute> {
        WelcomeRoot(onNavigateToLogin = { navController.navigate(LoginRoute) })
    }
    composable<LoginRoute> {
        LoginRoot(
            onNavigateToStravaConnect = {
                navController.navigate(StravaConnectRoute) {
                    popUpTo<WelcomeRoute> { inclusive = true }
                }
            },
            onNavigateToHome = {
                navController.navigate(HomeRoute) {
                    popUpTo<WelcomeRoute> { inclusive = true }
                }
            },
            onNavigateToSignUp = { navController.navigate(SignUpRoute) }
        )
    }
    composable<SignUpRoute> {
        SignUpRoot(
            onNavigateToEmailVerification = { email ->
                navController.navigate(EmailVerificationRoute(email)) {
                    popUpTo<WelcomeRoute> { inclusive = true }
                }
            },
            onNavigateToLogin = { navController.popBackStack() }
        )
    }
    composable<EmailVerificationRoute> { backStackEntry ->
        val route: EmailVerificationRoute = backStackEntry.toRoute()
        EmailVerificationRoot(
            email = route.email,
            onBackToLogin = {
                navController.navigate(LoginRoute) {
                    popUpTo<EmailVerificationRoute> { inclusive = true }
                }
            }
        )
    }
    composable<StravaConnectRoute> {
        StravaConnectRoot(
            onNavigateToLocationPermission = {
                navController.navigate(OnboardingLocationRoute) {
                    popUpTo<StravaConnectRoute> { inclusive = true }
                }
            }
        )
    }
    composable<OnboardingLocationRoute> {
        LocationPermissionRoot(
            onNavigateToNotifications = {
                navController.navigate(OnboardingNotificationsRoute) {
                    popUpTo<OnboardingLocationRoute> { inclusive = true }
                }
            }
        )
    }
    composable<OnboardingNotificationsRoute> {
        NotificationPermissionRoot(
            onNavigateToProfileSetup = {
                navController.navigate(OnboardingProfileRoute) {
                    popUpTo<OnboardingNotificationsRoute> { inclusive = true }
                }
            }
        )
    }
    composable<OnboardingProfileRoute> {
        ProfileSetupRoot(
            onNavigateToHome = {
                navController.navigate(HomeRoute) {
                    popUpTo<OnboardingProfileRoute> { inclusive = true }
                }
            }
        )
    }
    composable<SearchRoute> {
        SearchRoot(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToRunDetail = { runId -> navController.navigate(RunDetailRoute(runId)) },
            onNavigateToUserProfile = { userId -> navController.navigate(UserProfileRoute(userId)) },
        )
    }
    composable<HomeRoute> {
        HomeRoot(
            onNavigateToRunDetail = { runId ->
                navController.navigate(RunDetailRoute(runId))
            },
            onNavigateToCreateRun = {
                navController.navigate(CreateRunRoute)
            },
            onNavigateToSearch = {
                navController.navigate(SearchRoute)
            },
        )
    }
    composable<RunDetailRoute> {
        RunDetailRoot(onNavigateBack = { navController.popBackStack() })
    }
    composable<CreateRunRoute> {
        CreateRunRoot(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToRunDetail = { runId ->
                navController.navigate(RunDetailRoute(runId)) {
                    popUpTo<CreateRunRoute> { inclusive = true }
                }
            },
        )
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
