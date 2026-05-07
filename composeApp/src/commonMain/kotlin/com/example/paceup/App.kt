package com.example.paceup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.paceup.feature.appversion.AppVersionAction
import com.example.paceup.feature.appversion.AppVersionViewModel
import com.example.paceup.feature.appversion.VersionCheckState
import com.example.paceup.navigation.WelcomeRoute
import com.example.paceup.navigation.appGraph
import com.example.paceup.ui.ForceUpdateDialog
import com.example.paceup.ui.SoftUpdateBanner
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val PaceUpColorScheme = darkColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFF9FAFB),
    primaryContainer = Color(0xFF1A73E8),
    onPrimaryContainer = Color(0xFFF9FAFB),
    secondary = Color(0xFF4FC3F7),
    onSecondary = Color(0xFF0D1B2A),
    background = Color(0xFF0D1B2A),
    onBackground = Color(0xFFF9FAFB),
    surface = Color(0xFF1F2937),
    onSurface = Color(0xFFF9FAFB),
    error = Color(0xFFEF4444),
    onError = Color(0xFFF9FAFB),
)

/**
 * Root composable. Hosts the NavHost and version gate for all PaceUp screens.
 * TODO(paceup): add Supabase session check in Task 1.5 to route to HomeRoute when already logged in.
 *
 * @param appVersion Current app version string (e.g. "1.0.0"), provided by the platform entry point.
 * @param onOpenAppStore Platform-specific callback to open the app store listing.
 */
@Composable
fun App(appVersion: String, onOpenAppStore: () -> Unit) {
    MaterialTheme(colorScheme = PaceUpColorScheme) {
        val versionViewModel: AppVersionViewModel = koinViewModel { parametersOf(appVersion) }
        val versionState by versionViewModel.state.collectAsStateWithLifecycle()

        val navController = rememberNavController()

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (versionState.versionCheck is VersionCheckState.SoftUpdate &&
                    !versionState.softUpdateDismissed
                ) {
                    SoftUpdateBanner(
                        message = (versionState.versionCheck as VersionCheckState.SoftUpdate).message,
                        onDismiss = { versionViewModel.onAction(AppVersionAction.DismissSoftUpdate) },
                        onOpenStore = onOpenAppStore
                    )
                }

                NavHost(navController = navController, startDestination = WelcomeRoute) {
                    appGraph(navController)
                }
            }

            if (versionState.versionCheck is VersionCheckState.ForceUpdate) {
                ForceUpdateDialog(
                    message = (versionState.versionCheck as VersionCheckState.ForceUpdate).message,
                    onOpenStore = onOpenAppStore
                )
            }
        }
    }
}
