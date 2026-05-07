package com.example.paceup

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.paceup.navigation.WelcomeRoute
import com.example.paceup.navigation.appGraph

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
 * Root composable. Hosts the NavHost for all PaceUp screens.
 * TODO(paceup): add Supabase session check in Task 1.5 to route to HomeRoute when already logged in.
 */
@Composable
fun App() {
    MaterialTheme(colorScheme = PaceUpColorScheme) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = WelcomeRoute) {
            appGraph(navController)
        }
    }
}
